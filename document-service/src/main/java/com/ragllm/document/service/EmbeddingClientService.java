package com.ragllm.document.service;

import com.ragllm.common.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Client for generating embeddings via the internal embedding microservice.
 * Routes through embedding-service which has caching, metrics, and the OpenAI API key.
 */
@Service
public class EmbeddingClientService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClientService.class);

    private final WebClient webClient;

    public EmbeddingClientService(
            @Value("${embedding-service.url:http://localhost:8083}") String embeddingServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(embeddingServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /**
     * Generate embedding vector for a single text input via the embedding microservice.
     */
    @SuppressWarnings("unchecked")
    public float[] generateEmbedding(String text) {
        try {
            Map<String, String> request = Map.of("text", text);

            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/embeddings/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(60));

            if (response == null || !response.containsKey("data")) {
                throw new DocumentProcessingException("Empty response from embedding service");
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Number> embeddingList = (List<Number>) data.get("embedding");

            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }

            log.debug("Generated embedding of dimension {} for text of length {}", embedding.length, text.length());
            return embedding;
        } catch (Exception e) {
            log.error("Failed to generate embedding: {}", e.getMessage());
            throw new DocumentProcessingException("Failed to generate embedding", e);
        }
    }

    /**
     * Generate embeddings for multiple texts in a batch via the embedding microservice.
     */
    @SuppressWarnings("unchecked")
    public List<float[]> generateEmbeddings(List<String> texts) {
        try {
            Map<String, List<String>> request = Map.of("texts", texts);

            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/embeddings/batch")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(120));

            if (response == null || !response.containsKey("data")) {
                throw new DocumentProcessingException("Empty response from embedding service");
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<List<Number>> embeddingsList = (List<List<Number>>) data.get("embeddings");
            return embeddingsList.stream().map(embeddingList -> {
                float[] embedding = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    embedding[i] = embeddingList.get(i).floatValue();
                }
                return embedding;
            }).toList();
        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to generate batch embeddings", e);
        }
    }

    /**
     * Convert float array to pgvector-compatible string format.
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
}
