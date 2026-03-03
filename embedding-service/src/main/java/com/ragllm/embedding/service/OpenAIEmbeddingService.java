package com.ragllm.embedding.service;

import com.ragllm.common.exception.DocumentProcessingException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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
 * Service for generating embeddings via OpenAI API with:
 * - Retry logic for API failures
 * - Caching for repeated texts
 * - Prometheus metrics
 * - Rate limiting awareness
 * - Multi-model support
 */
@Service
public class OpenAIEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbeddingService.class);
    public static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";
    private static final int MAX_BATCH_SIZE = 100;

    /** Allowed embedding models with their dimensions */
    public static final Map<String, Integer> MODEL_DIMENSIONS = Map.of(
            "text-embedding-3-small", 1536,
            "text-embedding-3-large", 3072,
            "text-embedding-ada-002", 1536
    );

    public static final Set<String> ALLOWED_MODELS = MODEL_DIMENSIONS.keySet();

    private final WebClient webClient;
    private final Counter embeddingRequestCounter;
    private final Counter embeddingErrorCounter;
    private final Timer embeddingLatencyTimer;
    private final boolean devMode;

    public OpenAIEmbeddingService(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            MeterRegistry meterRegistry) {

        this.devMode = (apiKey == null || apiKey.isBlank());
        if (devMode) {
            log.warn("No OpenAI API key configured – running in DEV MODE with random embeddings");
        }

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        this.embeddingRequestCounter = Counter.builder("embedding.requests.total")
                .description("Total embedding API requests")
                .register(meterRegistry);

        this.embeddingErrorCounter = Counter.builder("embedding.errors.total")
                .description("Total embedding API errors")
                .register(meterRegistry);

        this.embeddingLatencyTimer = Timer.builder("embedding.latency")
                .description("Embedding generation latency")
                .register(meterRegistry);
    }

    /**
     * Generate a deterministic-ish random embedding for development/testing.
     * Uses text hashCode as seed so identical text returns identical vectors.
     */
    private float[] generateDevEmbedding(String text) {
        int dim = MODEL_DIMENSIONS.getOrDefault(DEFAULT_EMBEDDING_MODEL, 1536);
        java.util.Random rng = new java.util.Random(text.hashCode());
        float[] embedding = new float[dim];
        for (int i = 0; i < dim; i++) {
            embedding[i] = (float) rng.nextGaussian() * 0.1f;
        }
        log.debug("DEV MODE: generated random embedding for text of length {}", text.length());
        return embedding;
    }

    /**
     * Generate embedding for a single text with caching (default model).
     */
    @Cacheable(value = "embeddingCache", key = "#text.hashCode()")
    public float[] generateEmbedding(String text) {
        return generateEmbedding(text, null);
    }

    /**
     * Generate embedding for a single text with a specific model.
     */
    public float[] generateEmbedding(String text, String model) {
        String resolvedModel = resolveModel(model);
        if (devMode) {
            embeddingRequestCounter.increment();
            return generateDevEmbedding(text);
        }
        return embeddingLatencyTimer.record(() -> {
            embeddingRequestCounter.increment();
            try {
                return callEmbeddingApi(text, resolvedModel);
            } catch (Exception e) {
                embeddingErrorCounter.increment();
                throw e;
            }
        });
    }

    /**
     * Generate embeddings for multiple texts in batch.
     */
    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        if (texts.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size exceeds maximum of " + MAX_BATCH_SIZE);
        }

        if (devMode) {
            embeddingRequestCounter.increment();
            return texts.stream().map(this::generateDevEmbedding).toList();
        }

        return embeddingLatencyTimer.record(() -> {
            embeddingRequestCounter.increment();
            try {
                return callBatchEmbeddingApi(texts);
            } catch (Exception e) {
                embeddingErrorCounter.increment();
                throw e;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private float[] callEmbeddingApi(String text, String model) {
        Map<String, Object> request = Map.of(
                "input", text,
                "model", model
        );

        log.info("Calling OpenAI embedding API for text of length {} with model {}", text.length(), model);
        Map<String, Object> response = webClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .doBeforeRetry(signal -> log.warn("Retrying embedding API call, attempt {}", signal.totalRetries() + 1)))
                .block(Duration.ofSeconds(120));

        log.info("OpenAI embedding API returned successfully");
        return extractEmbedding(response);
    }

    @SuppressWarnings("unchecked")
    private List<float[]> callBatchEmbeddingApi(List<String> texts) {
        Map<String, Object> request = Map.of(
                "input", texts,
                "model", DEFAULT_EMBEDDING_MODEL
        );

        log.info("Calling OpenAI batch embedding API for {} texts", texts.size());
        Map<String, Object> response = webClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .doBeforeRetry(signal -> log.warn("Retrying batch embedding API call, attempt {}", signal.totalRetries() + 1)))
                .block(Duration.ofSeconds(180));

        if (response == null || !response.containsKey("data")) {
            throw new DocumentProcessingException("Empty response from embedding API");
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        return data.stream().map(this::parseEmbeddingFromData).toList();
    }

    @SuppressWarnings("unchecked")
    private float[] extractEmbedding(Map<String, Object> response) {
        if (response == null || !response.containsKey("data")) {
            throw new DocumentProcessingException("Empty response from embedding API");
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        return parseEmbeddingFromData(data.get(0));
    }

    @SuppressWarnings("unchecked")
    private float[] parseEmbeddingFromData(Map<String, Object> data) {
        List<Double> embeddingList = (List<Double>) data.get("embedding");
        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }
        return embedding;
    }

    /**
     * Convert float array to pgvector-compatible string.
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

    /**
     * Resolve and validate model name.
     */
    public String resolveModel(String requested) {
        if (requested == null || requested.isBlank()) return DEFAULT_EMBEDDING_MODEL;
        if (ALLOWED_MODELS.contains(requested)) return requested;
        log.warn("Requested embedding model '{}' is not allowed, falling back to '{}'", requested, DEFAULT_EMBEDDING_MODEL);
        return DEFAULT_EMBEDDING_MODEL;
    }
}
