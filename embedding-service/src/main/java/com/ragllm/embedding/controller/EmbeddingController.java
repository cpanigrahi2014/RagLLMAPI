package com.ragllm.embedding.controller;

import com.ragllm.common.dto.ApiResponse;
import com.ragllm.embedding.service.GeminiEmbeddingService;
import com.ragllm.embedding.service.OpenAIEmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/embeddings")
public class EmbeddingController {

    private final OpenAIEmbeddingService embeddingService;
    private final GeminiEmbeddingService geminiEmbeddingService;

    public EmbeddingController(OpenAIEmbeddingService embeddingService,
                               GeminiEmbeddingService geminiEmbeddingService) {
        this.embeddingService = embeddingService;
        this.geminiEmbeddingService = geminiEmbeddingService;
    }

    /**
     * Generate embedding for a single text with optional model selection.
     * Routes to Gemini or OpenAI based on the requested model.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateEmbedding(
            @RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Text is required"));
        }

        String model = request.get("model");

        // Route to Gemini embedding service if applicable
        if (GeminiEmbeddingService.isGeminiEmbeddingModel(model) && geminiEmbeddingService.isAvailable()) {
            float[] embedding = geminiEmbeddingService.generateEmbedding(text, model);
            Map<String, Object> result = Map.of(
                    "embedding", embedding,
                    "dimension", embedding.length,
                    "model", model
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        }

        // Default: OpenAI
        String resolvedModel = embeddingService.resolveModel(model);
        float[] embedding = embeddingService.generateEmbedding(text, resolvedModel);
        Map<String, Object> result = Map.of(
                "embedding", embedding,
                "dimension", embedding.length,
                "model", resolvedModel
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Generate embeddings for multiple texts in batch.
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateBatchEmbeddings(
            @RequestBody Map<String, List<String>> request) {
        List<String> texts = request.get("texts");
        if (texts == null || texts.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Texts list is required"));
        }

        List<float[]> embeddings = embeddingService.generateBatchEmbeddings(texts);
        Map<String, Object> result = Map.of(
                "embeddings", embeddings,
                "count", embeddings.size(),
                "model", OpenAIEmbeddingService.DEFAULT_EMBEDDING_MODEL
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
