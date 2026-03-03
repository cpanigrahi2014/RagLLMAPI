package com.ragllm.query.service;

import com.ragllm.common.dto.*;
import com.ragllm.common.entity.Book;
import com.ragllm.common.entity.Tenant;
import com.ragllm.common.exception.DocumentProcessingException;
import com.ragllm.common.exception.TenantNotFoundException;
import com.ragllm.common.security.UserPrincipal;
import com.ragllm.common.tenant.TenantContext;
import com.ragllm.common.util.InputSanitizer;
import com.ragllm.query.repository.BookRepository;
import com.ragllm.query.repository.TenantRepository;
import com.ragllm.query.repository.VectorSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI Studio Service — handles custom prompts, summarization, Q&A generation.
 */
@Service
public class AIStudioService {

    private static final Logger log = LoggerFactory.getLogger(AIStudioService.class);
    private static final int MAX_CONTEXT_CHUNKS = 30;

    // ── Prompt templates ──

    private static final String CUSTOM_PROMPT_WITH_CONTEXT = """
            You are an intelligent AI assistant for %s.
            The user has provided a custom prompt. Use the document context below to fulfill their request.
            
            DOCUMENT CONTEXT:
            %s
            
            USER PROMPT:
            %s
            """;

    private static final String CUSTOM_PROMPT_NO_CONTEXT = """
            You are an intelligent AI assistant for %s.
            Fulfill the user's request using your general knowledge.
            
            USER PROMPT:
            %s
            """;

    private static final String SUMMARIZE_BRIEF = """
            You are an intelligent AI assistant for %s.
            Summarize the following document content in 3-5 concise paragraphs.
            Focus on the key concepts, main ideas, and important facts.
            
            DOCUMENT CONTENT:
            %s
            """;

    private static final String SUMMARIZE_DETAILED = """
            You are an intelligent AI assistant for %s.
            Provide a comprehensive and detailed summary of the following document content.
            Include all major topics, sub-topics, key concepts, examples, and important details.
            Use headings and bullet points for clarity.
            
            DOCUMENT CONTENT:
            %s
            """;

    private static final String SUMMARIZE_BULLET = """
            You are an intelligent AI assistant for %s.
            Summarize the following document content as a structured bullet-point list.
            Group related ideas under headings. Keep each bullet concise but informative.
            
            DOCUMENT CONTENT:
            %s
            """;

    private static final String GENERATE_QA = """
            You are an intelligent AI assistant for %s.
            Based on the following document content, generate exactly %d question-answer pairs.
            Difficulty level: %s
            
            RULES:
            1. Questions should test understanding of the material.
            2. Answers should be detailed and accurate based on the document.
            3. For "mixed" difficulty, vary between easy, medium, and hard questions.
            4. Format EACH pair exactly as:
               Q1: [question]
               A1: [answer]
               
               Q2: [question]
               A2: [answer]
               ... and so on.
            
            DOCUMENT CONTENT:
            %s
            """;

    private final VectorSearchRepository vectorSearchRepository;
    private final BookRepository bookRepository;
    private final TenantRepository tenantRepository;
    private final OpenAIClient openAIClient;
    private final GeminiClient geminiClient;
    private final ClaudeClient claudeClient;
    private final UsageTrackingService usageTrackingService;

    public AIStudioService(VectorSearchRepository vectorSearchRepository,
                           BookRepository bookRepository,
                           TenantRepository tenantRepository,
                           OpenAIClient openAIClient,
                           GeminiClient geminiClient,
                           ClaudeClient claudeClient,
                           UsageTrackingService usageTrackingService) {
        this.vectorSearchRepository = vectorSearchRepository;
        this.bookRepository = bookRepository;
        this.tenantRepository = tenantRepository;
        this.openAIClient = openAIClient;
        this.geminiClient = geminiClient;
        this.claudeClient = claudeClient;
        this.usageTrackingService = usageTrackingService;
    }

    // ═══════════════════════════════════════════════════
    //  1. Custom Prompt
    // ═══════════════════════════════════════════════════

    public StudioResponse executeCustomPrompt(CustomPromptRequest request, UserPrincipal principal) {
        long startTime = System.currentTimeMillis();
        UUID tenantId = principal.getTenantId();
        UUID userId = principal.getUserId();

        usageTrackingService.enforceQuota(tenantId);
        Tenant tenant = getTenant(tenantId);

        String sanitizedPrompt = InputSanitizer.sanitizeQuery(request.getPrompt());
        String systemPrompt;

        if (request.getBookId() != null && !request.getBookId().isBlank()
                && Boolean.TRUE.equals(request.getUseDocumentContext())) {
            String context = getBookContext(UUID.fromString(request.getBookId()), tenantId);
            systemPrompt = String.format(CUSTOM_PROMPT_WITH_CONTEXT, tenant.getName(), context, sanitizedPrompt);
        } else {
            systemPrompt = String.format(CUSTOM_PROMPT_NO_CONTEXT, tenant.getName(), sanitizedPrompt);
        }

        OpenAIClient.ChatCompletionResult completion = callLLM(systemPrompt, sanitizedPrompt, request.getChatModel());

        long responseTime = System.currentTimeMillis() - startTime;
        usageTrackingService.logUsage(tenantId, userId, "studio:custom-prompt",
                completion.promptTokens(), completion.completionTokens(), completion.model(), responseTime);

        log.info("Custom prompt completed for tenant {} in {}ms ({} tokens)", tenantId, responseTime, completion.totalTokens());

        return StudioResponse.builder()
                .result(completion.content())
                .tokensUsed(completion.totalTokens())
                .responseTimeMs(responseTime)
                .chatModel(completion.model())
                .type("custom-prompt")
                .build();
    }

    // ═══════════════════════════════════════════════════
    //  2. Summarize Document
    // ═══════════════════════════════════════════════════

    public StudioResponse summarizeDocument(SummarizeRequest request, UserPrincipal principal) {
        long startTime = System.currentTimeMillis();
        UUID tenantId = principal.getTenantId();
        UUID userId = principal.getUserId();

        usageTrackingService.enforceQuota(tenantId);
        Tenant tenant = getTenant(tenantId);

        String context = getBookContext(UUID.fromString(request.getBookId()), tenantId);

        String template = switch (request.getStyle() != null ? request.getStyle() : "detailed") {
            case "brief" -> SUMMARIZE_BRIEF;
            case "bullet-points" -> SUMMARIZE_BULLET;
            default -> SUMMARIZE_DETAILED;
        };
        String systemPrompt = String.format(template, tenant.getName(), context);

        OpenAIClient.ChatCompletionResult completion = callLLM(systemPrompt, "Summarize this document.", request.getChatModel());

        long responseTime = System.currentTimeMillis() - startTime;
        usageTrackingService.logUsage(tenantId, userId, "studio:summarize",
                completion.promptTokens(), completion.completionTokens(), completion.model(), responseTime);

        log.info("Summarization completed for tenant {} in {}ms ({} tokens)", tenantId, responseTime, completion.totalTokens());

        return StudioResponse.builder()
                .result(completion.content())
                .tokensUsed(completion.totalTokens())
                .responseTimeMs(responseTime)
                .chatModel(completion.model())
                .type("summarize")
                .build();
    }

    // ═══════════════════════════════════════════════════
    //  3. Generate Q&A from Document
    // ═══════════════════════════════════════════════════

    public StudioResponse generateQA(GenerateQARequest request, UserPrincipal principal) {
        long startTime = System.currentTimeMillis();
        UUID tenantId = principal.getTenantId();
        UUID userId = principal.getUserId();

        usageTrackingService.enforceQuota(tenantId);
        Tenant tenant = getTenant(tenantId);

        String context = getBookContext(UUID.fromString(request.getBookId()), tenantId);

        int count = request.getCount() != null ? Math.min(request.getCount(), 30) : 10;
        String difficulty = request.getDifficulty() != null ? request.getDifficulty() : "mixed";

        String systemPrompt = String.format(GENERATE_QA, tenant.getName(), count, difficulty, context);

        OpenAIClient.ChatCompletionResult completion = callLLM(systemPrompt,
                "Generate " + count + " question-answer pairs from this document.", request.getChatModel());

        long responseTime = System.currentTimeMillis() - startTime;
        usageTrackingService.logUsage(tenantId, userId, "studio:generate-qa",
                completion.promptTokens(), completion.completionTokens(), completion.model(), responseTime);

        // Parse Q&A pairs from the response
        List<StudioResponse.QAPair> qaPairs = parseQAPairs(completion.content());

        log.info("Q&A generation completed for tenant {}: {} pairs in {}ms ({} tokens)",
                tenantId, qaPairs.size(), responseTime, completion.totalTokens());

        return StudioResponse.builder()
                .result(completion.content())
                .qaPairs(qaPairs)
                .tokensUsed(completion.totalTokens())
                .responseTimeMs(responseTime)
                .chatModel(completion.model())
                .type("generate-qa")
                .build();
    }

    // ═══════════════════════════════════════════════════
    //  4. Text-to-Speech (OpenAI TTS API)
    // ═══════════════════════════════════════════════════

    public byte[] generateSpeech(TTSRequest request) {
        return openAIClient.textToSpeech(request.getText(), request.getVoice(),
                request.getModel(), request.getSpeed());
    }

    // ═══════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════

    private String getBookContext(UUID bookId, UUID tenantId) {
        Book book = bookRepository.findByIdAndTenantId(bookId, tenantId)
                .orElseThrow(() -> new DocumentProcessingException("Book not found or access denied"));

        List<Object[]> chunks = vectorSearchRepository.findChunkContentByBookId(bookId, tenantId, MAX_CONTEXT_CHUNKS);

        if (chunks.isEmpty()) {
            throw new DocumentProcessingException("No processed content found for this book. Ensure the book has finished processing.");
        }

        return chunks.stream()
                .map(row -> {
                    String content = (String) row[0];
                    Integer pageNumber = row[1] != null ? ((Number) row[1]).intValue() : null;
                    return content + (pageNumber != null ? "\n[Page " + pageNumber + "]" : "");
                })
                .collect(Collectors.joining("\n---\n"));
    }

    private OpenAIClient.ChatCompletionResult callLLM(String systemPrompt, String userMessage, String chatModel) {
        try {
            if (ClaudeClient.isClaudeModel(chatModel) && claudeClient.isAvailable()) {
                return claudeClient.chatCompletion(systemPrompt, userMessage, chatModel);
            } else if (GeminiClient.isGeminiModel(chatModel) && geminiClient.isAvailable()) {
                return geminiClient.chatCompletion(systemPrompt, userMessage, chatModel);
            } else {
                return openAIClient.chatCompletion(systemPrompt, userMessage, chatModel);
            }
        } catch (WebClientResponseException e) {
            int status = e.getStatusCode().value();
            String message;
            if (status == 429) {
                message = "AI model rate limit exceeded. Please wait a moment and try again.";
                log.warn("Upstream 429 rate limit for model {}: {}", chatModel, e.getMessage());
            } else if (status == 401 || status == 403) {
                message = "AI model authentication failed. Check API key configuration.";
                log.error("Upstream auth error {} for model {}: {}", status, chatModel, e.getMessage());
            } else {
                message = "AI model service error (" + status + "). Try again or use a different model.";
                log.error("Upstream error {} for model {}: {}", status, chatModel, e.getMessage());
            }
            throw new DocumentProcessingException(message);
        } catch (Exception e) {
            log.error("LLM call failed for model {}: {}", chatModel, e.getMessage(), e);
            throw new DocumentProcessingException("Failed to get response from AI model. Please try again or use a different model.");
        }
    }

    private Tenant getTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
    }

    /**
     * Parse LLM output into structured Q&A pairs.
     * Expects format: Q1: ... A1: ... Q2: ... A2: ...
     */
    private List<StudioResponse.QAPair> parseQAPairs(String text) {
        List<StudioResponse.QAPair> pairs = new java.util.ArrayList<>();
        // Split by Q followed by digit
        String[] parts = text.split("(?=Q\\d+:)");
        int num = 0;
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            // Extract question and answer
            int aIndex = -1;
            // Find A<number>: pattern
            for (int i = 0; i < part.length() - 2; i++) {
                if (part.charAt(i) == 'A' && Character.isDigit(part.charAt(i + 1))) {
                    int j = i + 2;
                    while (j < part.length() && Character.isDigit(part.charAt(j))) j++;
                    if (j < part.length() && part.charAt(j) == ':') {
                        aIndex = i;
                        break;
                    }
                }
            }
            if (aIndex > 0) {
                num++;
                String question = part.substring(0, aIndex).replaceFirst("Q\\d+:\\s*", "").trim();
                String answer = part.substring(aIndex).replaceFirst("A\\d+:\\s*", "").trim();
                pairs.add(StudioResponse.QAPair.builder()
                        .number(num)
                        .question(question)
                        .answer(answer)
                        .difficulty("mixed")
                        .build());
            }
        }
        return pairs;
    }
}
