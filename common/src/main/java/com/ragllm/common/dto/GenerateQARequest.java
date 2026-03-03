package com.ragllm.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQARequest {

    @NotBlank(message = "Book ID is required")
    private String bookId;

    /** Chat model to use */
    private String chatModel;

    /** Number of Q&A pairs to generate */
    @Builder.Default
    private Integer count = 10;

    /** Difficulty: easy, medium, hard, mixed */
    @Builder.Default
    private String difficulty = "mixed";

    /** Embedding model for chunk retrieval */
    private String embeddingModel;
}
