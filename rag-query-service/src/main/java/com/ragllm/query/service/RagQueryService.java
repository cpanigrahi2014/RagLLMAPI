package com.ragllm.query.service;

import com.ragllm.common.dto.QueryRequest;
import com.ragllm.common.dto.QueryResponse;
import com.ragllm.common.entity.Tenant;
import com.ragllm.common.exception.TenantNotFoundException;
import com.ragllm.common.security.UserPrincipal;
import com.ragllm.common.tenant.TenantContext;
import com.ragllm.common.util.InputSanitizer;
import com.ragllm.query.repository.TenantRepository;
import com.ragllm.query.repository.VectorSearchRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core RAG Query Service that orchestrates:
 * 1. Quota enforcement
 * 2. Query embedding generation
 * 3. Vector similarity search (tenant-isolated)
 * 4. Prompt construction with context
 * 5. LLM completion
 * 6. Usage tracking
 */
@Service
public class RagQueryService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);
    private static final int DEFAULT_TOP_K = 5;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are an intelligent educational assistant for %s.
            %s
            RULES:
            1. Answer ONLY using the provided context below.
            2. If the answer is not found in the context, respond: "This information is not available in the uploaded materials."
            3. Be concise, accurate, and helpful for students.
            4. Cite the source (book/chapter/page) when possible.
            5. Format answers clearly with bullet points or numbered lists when appropriate.
            6. You can answer questions on any subject — Science, Math, History, English, Computer Science, etc.
            
            CONTEXT:
            %s
            """;

    private static final String DIRECT_SYSTEM_PROMPT = """
            You are an intelligent educational assistant for %s.
            %s
            RULES:
            1. Use your general knowledge to answer the student's question.
            2. Be concise, accurate, and helpful for students.
            3. Format answers clearly with bullet points or numbered lists when appropriate.
            4. You can answer questions on any subject — Science, Math, History, English, Computer Science, etc.
            5. If the question is unclear, ask for clarification.
            """;

    private final VectorSearchRepository vectorSearchRepository;
    private final TenantRepository tenantRepository;
    private final OpenAIClient openAIClient;
    private final GeminiClient geminiClient;
    private final ClaudeClient claudeClient;
    private final UsageTrackingService usageTrackingService;
    private final Counter queryCounter;
    private final Timer queryLatencyTimer;

    public RagQueryService(VectorSearchRepository vectorSearchRepository,
                            TenantRepository tenantRepository,
                            OpenAIClient openAIClient,
                            GeminiClient geminiClient,
                            ClaudeClient claudeClient,
                            UsageTrackingService usageTrackingService,
                            MeterRegistry meterRegistry) {
        this.vectorSearchRepository = vectorSearchRepository;
        this.tenantRepository = tenantRepository;
        this.openAIClient = openAIClient;
        this.geminiClient = geminiClient;
        this.claudeClient = claudeClient;
        this.usageTrackingService = usageTrackingService;

        this.queryCounter = Counter.builder("rag.queries.total")
                .description("Total RAG queries processed")
                .register(meterRegistry);

        this.queryLatencyTimer = Timer.builder("rag.query.latency")
                .description("RAG query end-to-end latency")
                .register(meterRegistry);
    }

    /**
     * Execute a RAG query with full pipeline.
     */
    public QueryResponse query(QueryRequest request, UserPrincipal principal) {
        return queryLatencyTimer.record(() -> executeQuery(request, principal));
    }

    /**
     * Execute query with Redis caching for repeated questions.
     * Cache key includes tenantId + query to ensure tenant isolation.
     */
    @Cacheable(value = "queryCache",
               key = "#principal.tenantId + '_' + #request.query.hashCode()",
               unless = "#result == null")
    public QueryResponse queryCached(QueryRequest request, UserPrincipal principal) {
        return executeQuery(request, principal);
    }

    private QueryResponse executeQuery(QueryRequest request, UserPrincipal principal) {
        long startTime = System.currentTimeMillis();
        UUID tenantId = principal.getTenantId();
        UUID userId = principal.getUserId();

        queryCounter.increment();

        // 1. Sanitize input
        String sanitizedQuery = InputSanitizer.sanitizeQuery(request.getQuery());

        // 2. Enforce quota
        usageTrackingService.enforceQuota(tenantId);

        // 3. Get tenant info
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        // --- NON-RAG (direct LLM) path ---
        Boolean useRag = request.getUseRag();
        if (useRag != null && !useRag) {
            return executeDirectQuery(request, tenant, tenantId, userId, sanitizedQuery, startTime);
        }

        // 4. Generate query embedding (route to correct provider)
        String embeddingModel = request.getEmbeddingModel();
        float[] queryEmbedding;
        if (GeminiClient.isGeminiModel(embeddingModel) && geminiClient.isAvailable()) {
            queryEmbedding = geminiClient.generateQueryEmbedding(sanitizedQuery, embeddingModel);
        } else {
            queryEmbedding = openAIClient.generateQueryEmbedding(sanitizedQuery, embeddingModel);
        }
        String vectorString = OpenAIClient.toVectorString(queryEmbedding);

        // 5. Perform vector similarity search (tenant-isolated, optionally filtered by subject/class)
        int topK = request.getMaxResults() != null ? request.getMaxResults() : DEFAULT_TOP_K;
        String subject = request.getSubject();
        Integer classLevel = request.getClassLevel();

        List<Object[]> results;
        try {
            if (subject != null && !subject.isBlank() && classLevel != null) {
                results = vectorSearchRepository.findSimilarChunksBySubjectAndClass(
                        tenantId, vectorString, topK, subject, classLevel);
            } else if (subject != null && !subject.isBlank()) {
                results = vectorSearchRepository.findSimilarChunksBySubject(
                        tenantId, vectorString, topK, subject);
            } else if (classLevel != null) {
                results = vectorSearchRepository.findSimilarChunksByClass(
                        tenantId, vectorString, topK, classLevel);
            } else {
                results = vectorSearchRepository.findSimilarChunksWithScore(
                        tenantId, vectorString, topK);
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("different vector dimensions")) {
                log.warn("Vector dimension mismatch for tenant {} — query embedding dimensions don't match stored embeddings. embeddingModel={}", tenantId, embeddingModel);
                return QueryResponse.builder()
                        .answer("**Embedding dimension mismatch.** The selected embedding model produces vectors of a different size than what your documents were indexed with. Please select the same embedding model that was used when uploading your books (likely **text-embedding-3-small**).")
                        .sources(List.of())
                        .tokensUsed(0)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .chatModel(request.getChatModel())
                        .embeddingModel(embeddingModel)
                        .build();
            }
            throw e;
        }

        // 6. Build context from retrieved chunks
        StringBuilder contextBuilder = new StringBuilder();
        List<QueryResponse.SourceChunk> sources = results.stream()
                .map(row -> {
                    String chunkId = row[0].toString();
                    String content = (String) row[1];
                    Integer pageNumber = row[2] != null ? ((Number) row[2]).intValue() : null;
                    Integer chunkIndex = row[3] != null ? ((Number) row[3]).intValue() : null;
                    String chapterId = row[4] != null ? row[4].toString() : null;
                    double similarity = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;

                    contextBuilder.append("---\n")
                            .append(content)
                            .append("\n[Page: ").append(pageNumber != null ? pageNumber : "N/A").append("]\n\n");

                    return QueryResponse.SourceChunk.builder()
                            .chunkId(chunkId)
                            .content(content.length() > 200 ? content.substring(0, 200) + "..." : content)
                            .pageNumber(pageNumber)
                            .similarityScore(similarity)
                            .build();
                })
                .collect(Collectors.toList());

        // 7. Construct system prompt
        String context = contextBuilder.toString();
        if (context.isBlank()) {
            return QueryResponse.builder()
                    .answer("No relevant content found in the uploaded materials for your query.")
                    .sources(List.of())
                    .tokensUsed(0)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .chatModel(OpenAIClient.DEFAULT_CHAT_MODEL)
                    .embeddingModel(embeddingModel != null ? embeddingModel : OpenAIClient.DEFAULT_EMBEDDING_MODEL)
                    .build();
        }

        String subjectInfo = "";
        if (subject != null && !subject.isBlank()) {
            subjectInfo = "You are specialized in " + subject + ". ";
            if (classLevel != null) {
                subjectInfo += "The student is in class " + classLevel + ". ";
            }
        } else if (classLevel != null) {
            subjectInfo = "The student is in class " + classLevel + ". ";
        }

        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, tenant.getName(), subjectInfo, context);

        // 8. Call LLM (route to correct provider)
        String chatModel = request.getChatModel();
        OpenAIClient.ChatCompletionResult completion;
        if (ClaudeClient.isClaudeModel(chatModel) && claudeClient.isAvailable()) {
            completion = claudeClient.chatCompletion(systemPrompt, sanitizedQuery, chatModel);
        } else if (GeminiClient.isGeminiModel(chatModel) && geminiClient.isAvailable()) {
            completion = geminiClient.chatCompletion(systemPrompt, sanitizedQuery, chatModel);
        } else {
            completion = openAIClient.chatCompletion(systemPrompt, sanitizedQuery, chatModel);
        }

        // 9. Log usage
        long responseTime = System.currentTimeMillis() - startTime;
        usageTrackingService.logUsage(
                tenantId, userId, sanitizedQuery,
                completion.promptTokens(), completion.completionTokens(),
                completion.model(), responseTime
        );

        log.info("RAG query completed for tenant {} in {}ms ({} tokens, chat={}, embed={})",
                tenantId, responseTime, completion.totalTokens(), completion.model(),
                embeddingModel != null ? embeddingModel : OpenAIClient.DEFAULT_EMBEDDING_MODEL);

        // 10. Return response
        return QueryResponse.builder()
                .answer(completion.content())
                .sources(sources)
                .tokensUsed(completion.totalTokens())
                .responseTimeMs(responseTime)
                .chatModel(completion.model())
                .embeddingModel(embeddingModel != null ? embeddingModel : OpenAIClient.DEFAULT_EMBEDDING_MODEL)
                .build();
    }

    /**
     * Execute a direct (non-RAG) query — skip embedding/vector search, call LLM with general knowledge.
     */
    private QueryResponse executeDirectQuery(QueryRequest request, Tenant tenant,
                                              UUID tenantId, UUID userId,
                                              String sanitizedQuery, long startTime) {
        String subject = request.getSubject();
        Integer classLevel = request.getClassLevel();
        String subjectInfo = "";
        if (subject != null && !subject.isBlank()) {
            subjectInfo = "You are specialized in " + subject + ". ";
            if (classLevel != null) {
                subjectInfo += "The student is in class " + classLevel + ". ";
            }
        } else if (classLevel != null) {
            subjectInfo = "The student is in class " + classLevel + ". ";
        }

        String systemPrompt = String.format(DIRECT_SYSTEM_PROMPT, tenant.getName(), subjectInfo);

        String chatModel = request.getChatModel();
        OpenAIClient.ChatCompletionResult completion;
        if (ClaudeClient.isClaudeModel(chatModel) && claudeClient.isAvailable()) {
            completion = claudeClient.chatCompletion(systemPrompt, sanitizedQuery, chatModel);
        } else if (GeminiClient.isGeminiModel(chatModel) && geminiClient.isAvailable()) {
            completion = geminiClient.chatCompletion(systemPrompt, sanitizedQuery, chatModel);
        } else {
            completion = openAIClient.chatCompletion(systemPrompt, sanitizedQuery, chatModel);
        }

        long responseTime = System.currentTimeMillis() - startTime;
        usageTrackingService.logUsage(
                tenantId, userId, sanitizedQuery,
                completion.promptTokens(), completion.completionTokens(),
                completion.model(), responseTime
        );

        log.info("Direct (non-RAG) query completed for tenant {} in {}ms ({} tokens, chat={})",
                tenantId, responseTime, completion.totalTokens(), completion.model());

        return QueryResponse.builder()
                .answer(completion.content())
                .sources(List.of())
                .tokensUsed(completion.totalTokens())
                .responseTimeMs(responseTime)
                .chatModel(completion.model())
                .embeddingModel("N/A (direct mode)")
                .build();
    }
}
