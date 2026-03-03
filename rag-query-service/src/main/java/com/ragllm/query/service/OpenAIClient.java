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
 * Client for OpenAI Chat Completions and Embeddings API.
 */
@Service
public class OpenAIClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIClient.class);
    public static final String DEFAULT_CHAT_MODEL = "gpt-4.1-mini";
    public static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";

    /** Allowed chat models */
    public static final Set<String> ALLOWED_CHAT_MODELS = Set.of(
            "gpt-4.1-mini",
            "gpt-4.1-nano",
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4.1",
            "gpt-4-turbo",
            "o4-mini",
            "o3-mini"
    );

    /** Allowed embedding models */
    public static final Set<String> ALLOWED_EMBEDDING_MODELS = Set.of(
            "text-embedding-3-small",
            "text-embedding-3-large",
            "text-embedding-ada-002"
    );

    private final WebClient webClient;

    public OpenAIClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * Call chat completion API with system and user messages.
     */
    @SuppressWarnings("unchecked")
    public ChatCompletionResult chatCompletion(String systemPrompt, String userMessage) {
        return chatCompletion(systemPrompt, userMessage, null);
    }

    /**
     * Call chat completion API with a specific model.
     */
    @SuppressWarnings("unchecked")
    public ChatCompletionResult chatCompletion(String systemPrompt, String userMessage, String model) {
        String chatModel = resolveModel(model, ALLOWED_CHAT_MODELS, DEFAULT_CHAT_MODEL);
        log.info("Using chat model: {}", chatModel);

        Map<String, Object> request = Map.of(
                "model", chatModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.3,
                "max_tokens", 1024
        );

        long start = System.currentTimeMillis();

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5)))
                .block(Duration.ofSeconds(60));

        long elapsed = System.currentTimeMillis() - start;

        if (response == null) {
            throw new DocumentProcessingException("Empty response from chat API");
        }

        // Extract answer
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        // Extract token usage
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        int promptTokens = ((Number) usage.get("prompt_tokens")).intValue();
        int completionTokens = ((Number) usage.get("completion_tokens")).intValue();
        int totalTokens = ((Number) usage.get("total_tokens")).intValue();

        log.debug("Chat completion: {} tokens in {}ms (model={})", totalTokens, elapsed, chatModel);

        return new ChatCompletionResult(content, promptTokens, completionTokens, totalTokens, elapsed, chatModel);
    }

    /**
     * Generate embedding for a query string.
     */
    @SuppressWarnings("unchecked")
    public float[] generateQueryEmbedding(String text) {
        return generateQueryEmbedding(text, null);
    }

    /**
     * Generate embedding for a query string with a specific model.
     */
    @SuppressWarnings("unchecked")
    public float[] generateQueryEmbedding(String text, String model) {
        String embeddingModel = resolveModel(model, ALLOWED_EMBEDDING_MODELS, DEFAULT_EMBEDDING_MODEL);
        log.info("Using embedding model: {}", embeddingModel);

        Map<String, Object> request = Map.of(
                "input", text,
                "model", embeddingModel
        );

        Map<String, Object> response = webClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .block(Duration.ofSeconds(30));

        if (response == null || !response.containsKey("data")) {
            throw new DocumentProcessingException("Empty response from embedding API");
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<Double> embeddingList = (List<Double>) data.get(0).get("embedding");

        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }

        return embedding;
    }

    /**
     * Convert float array to pgvector string format.
     */
    public static String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public record ChatCompletionResult(
            String content,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long responseTimeMs,
            String model
    ) {}

    /**
     * Text-to-Speech via OpenAI TTS API.
     * Returns raw audio bytes (MP3).
     */
    public byte[] textToSpeech(String text, String voice, String model, Double speed) {
        if (voice == null || voice.isBlank()) voice = "nova";
        if (model == null || model.isBlank()) model = "tts-1";
        if (speed == null || speed < 0.25 || speed > 4.0) speed = 1.0;

        log.info("TTS request: model={}, voice={}, speed={}, textLength={}", model, voice, speed, text.length());

        Map<String, Object> request = Map.of(
                "model", model,
                "input", text,
                "voice", voice,
                "speed", speed
        );

        byte[] audioBytes = webClient.post()
                .uri("/audio/speech")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(byte[].class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(5)))
                .block(Duration.ofSeconds(60));

        if (audioBytes == null || audioBytes.length == 0) {
            throw new DocumentProcessingException("Empty response from TTS API");
        }

        log.info("TTS completed: {} bytes audio generated", audioBytes.length);
        return audioBytes;
    }

    /**
     * Resolve and validate model name. Falls back to default if null/blank/not allowed.
     */
    private String resolveModel(String requested, Set<String> allowed, String defaultModel) {
        if (requested == null || requested.isBlank()) return defaultModel;
        if (allowed.contains(requested)) return requested;
        log.warn("Requested model '{}' is not allowed, falling back to '{}'", requested, defaultModel);
        return defaultModel;
    }
}
