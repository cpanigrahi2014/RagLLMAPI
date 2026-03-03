package com.ragllm.query.service;

import com.ragllm.common.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client for Google Gemini Chat and Embeddings API.
 *
 * Chat endpoint:  POST /v1beta/models/{model}:generateContent?key={apiKey}
 * Embed endpoint: POST /v1beta/models/{model}:embedContent?key={apiKey}
 */
@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    public static final String DEFAULT_GEMINI_CHAT_MODEL = "gemini-2.0-flash";
    public static final String DEFAULT_GEMINI_EMBEDDING_MODEL = "gemini-embedding-001";

    public static final Set<String> ALLOWED_CHAT_MODELS = Set.of(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-pro",
            "gemini-1.5-flash"
    );

    public static final Set<String> ALLOWED_EMBEDDING_MODELS = Set.of(
            "gemini-embedding-001"
    );

    private final WebClient webClient;
    private final String apiKey;
    private final boolean available;

    public GeminiClient(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.available = apiKey != null && !apiKey.isBlank();

        if (!available) {
            log.warn("No Gemini API key configured – Gemini models will not be available");
        }

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Call Gemini generateContent with system instruction and user message.
     */
    @SuppressWarnings("unchecked")
    public OpenAIClient.ChatCompletionResult chatCompletion(String systemPrompt, String userMessage, String model) {
        if (!available) {
            throw new DocumentProcessingException("Gemini API key not configured");
        }

        String chatModel = resolveChatModel(model);
        log.info("Using Gemini chat model: {}", chatModel);

        // Build Gemini request format
        Map<String, Object> request = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", userMessage)))
                ),
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 1024
                )
        );

        long start = System.currentTimeMillis();

        Map<String, Object> response = webClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", chatModel, apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5)))
                .block(Duration.ofSeconds(90));

        long elapsed = System.currentTimeMillis() - start;

        if (response == null) {
            throw new DocumentProcessingException("Empty response from Gemini chat API");
        }

        // Extract answer from Gemini response
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new DocumentProcessingException("No candidates in Gemini response");
        }

        Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
        String content = (String) parts.get(0).get("text");

        // Extract token usage
        Map<String, Object> usageMetadata = (Map<String, Object>) response.get("usageMetadata");
        int promptTokens = usageMetadata != null && usageMetadata.containsKey("promptTokenCount")
                ? ((Number) usageMetadata.get("promptTokenCount")).intValue() : 0;
        int completionTokens = usageMetadata != null && usageMetadata.containsKey("candidatesTokenCount")
                ? ((Number) usageMetadata.get("candidatesTokenCount")).intValue() : 0;
        int totalTokens = usageMetadata != null && usageMetadata.containsKey("totalTokenCount")
                ? ((Number) usageMetadata.get("totalTokenCount")).intValue() : promptTokens + completionTokens;

        log.debug("Gemini chat completion: {} tokens in {}ms (model={})", totalTokens, elapsed, chatModel);

        return new OpenAIClient.ChatCompletionResult(content, promptTokens, completionTokens, totalTokens, elapsed, chatModel);
    }

    /**
     * Generate embedding via Gemini embedContent API.
     */
    @SuppressWarnings("unchecked")
    public float[] generateQueryEmbedding(String text, String model) {
        if (!available) {
            throw new DocumentProcessingException("Gemini API key not configured");
        }

        String embeddingModel = resolveEmbeddingModel(model);
        log.info("Using Gemini embedding model: {}", embeddingModel);

        Map<String, Object> request = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                )
        );

        Map<String, Object> response = webClient.post()
                .uri("/v1beta/models/{model}:embedContent?key={key}", embeddingModel, apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5)))
                .block(Duration.ofSeconds(30));

        if (response == null || !response.containsKey("embedding")) {
            throw new DocumentProcessingException("Empty response from Gemini embedding API");
        }

        Map<String, Object> embedding = (Map<String, Object>) response.get("embedding");
        List<Number> values = (List<Number>) embedding.get("values");

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }

        log.debug("Gemini embedding: {} dimensions for text of length {}", result.length, text.length());
        return result;
    }

    public String resolveChatModel(String requested) {
        if (requested == null || requested.isBlank()) return DEFAULT_GEMINI_CHAT_MODEL;
        if (ALLOWED_CHAT_MODELS.contains(requested)) return requested;
        log.warn("Requested Gemini chat model '{}' is not allowed, falling back to '{}'", requested, DEFAULT_GEMINI_CHAT_MODEL);
        return DEFAULT_GEMINI_CHAT_MODEL;
    }

    public String resolveEmbeddingModel(String requested) {
        if (requested == null || requested.isBlank()) return DEFAULT_GEMINI_EMBEDDING_MODEL;
        if (ALLOWED_EMBEDDING_MODELS.contains(requested)) return requested;
        log.warn("Requested Gemini embedding model '{}' is not allowed, falling back to '{}'", requested, DEFAULT_GEMINI_EMBEDDING_MODEL);
        return DEFAULT_GEMINI_EMBEDDING_MODEL;
    }

    /** Check if a model name is a Gemini model */
    public static boolean isGeminiModel(String model) {
        if (model == null) return false;
        return model.startsWith("gemini-");
    }
}
