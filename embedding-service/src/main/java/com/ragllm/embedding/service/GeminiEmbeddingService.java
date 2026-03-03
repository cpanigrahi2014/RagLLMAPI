package com.ragllm.embedding.service;

import com.ragllm.common.exception.DocumentProcessingException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
 * Service for generating embeddings via Google Gemini API.
 * Entry‑point: POST /v1beta/models/{model}:embedContent?key={apiKey}
 */
@Service
public class GeminiEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingService.class);
    public static final String DEFAULT_MODEL = "gemini-embedding-001";

    /** Gemini embedding models with their dimensions */
    public static final Map<String, Integer> MODEL_DIMENSIONS = Map.of(
            "gemini-embedding-001", 3072
    );

    public static final Set<String> ALLOWED_MODELS = MODEL_DIMENSIONS.keySet();

    private final WebClient webClient;
    private final String apiKey;
    private final boolean available;
    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Timer latencyTimer;

    public GeminiEmbeddingService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            MeterRegistry meterRegistry) {

        this.apiKey = apiKey;
        this.available = apiKey != null && !apiKey.isBlank();

        if (!available) {
            log.warn("No Gemini API key configured – Gemini embedding models will not be available");
        }

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        this.requestCounter = Counter.builder("gemini.embedding.requests.total")
                .description("Total Gemini embedding API requests")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("gemini.embedding.errors.total")
                .description("Total Gemini embedding API errors")
                .register(meterRegistry);

        this.latencyTimer = Timer.builder("gemini.embedding.latency")
                .description("Gemini embedding generation latency")
                .register(meterRegistry);
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Generate embedding for a single text.
     */
    @SuppressWarnings("unchecked")
    public float[] generateEmbedding(String text, String model) {
        if (!available) {
            throw new DocumentProcessingException("Gemini API key not configured");
        }

        String resolvedModel = resolveModel(model);
        requestCounter.increment();

        return latencyTimer.record(() -> {
            try {
                Map<String, Object> request = Map.of(
                        "model", "models/" + resolvedModel,
                        "content", Map.of(
                                "parts", List.of(Map.of("text", text))
                        )
                );

                log.info("Calling Gemini embedding API for text of length {} with model {}", text.length(), resolvedModel);

                Map<String, Object> response = webClient.post()
                        .uri("/v1beta/models/{model}:embedContent?key={key}", resolvedModel, apiKey)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(30))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                                .maxBackoff(Duration.ofSeconds(5))
                                .doBeforeRetry(signal -> log.warn("Retrying Gemini embedding API, attempt {}", signal.totalRetries() + 1)))
                        .block(Duration.ofSeconds(120));

                if (response == null || !response.containsKey("embedding")) {
                    throw new DocumentProcessingException("Empty response from Gemini embedding API");
                }

                Map<String, Object> embedding = (Map<String, Object>) response.get("embedding");
                List<Number> values = (List<Number>) embedding.get("values");

                float[] result = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    result[i] = values.get(i).floatValue();
                }

                log.info("Gemini embedding API returned {} dimensions", result.length);
                return result;
            } catch (DocumentProcessingException e) {
                errorCounter.increment();
                throw e;
            } catch (Exception e) {
                errorCounter.increment();
                throw new DocumentProcessingException("Gemini embedding API failed: " + e.getMessage(), e);
            }
        });
    }

    /** Check if a model name is a Gemini embedding model */
    public static boolean isGeminiEmbeddingModel(String model) {
        if (model == null) return false;
        return ALLOWED_MODELS.contains(model);
    }

    public String resolveModel(String requested) {
        if (requested == null || requested.isBlank()) return DEFAULT_MODEL;
        if (ALLOWED_MODELS.contains(requested)) return requested;
        log.warn("Requested Gemini embedding model '{}' not allowed, falling back to '{}'", requested, DEFAULT_MODEL);
        return DEFAULT_MODEL;
    }
}
