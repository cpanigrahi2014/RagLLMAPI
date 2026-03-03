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
public class SummarizeRequest {

    @NotBlank(message = "Book ID is required")
    private String bookId;

    /** Chat model to use for summarization */
    private String chatModel;

    /** Summary style: brief, detailed, bullet-points */
    @Builder.Default
    private String style = "detailed";

    /** Embedding model for chunk retrieval */
    private String embeddingModel;
}
