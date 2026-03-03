package com.ragllm.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomPromptRequest {

    @NotBlank(message = "Prompt is required")
    @Size(max = 5000, message = "Prompt must not exceed 5000 characters")
    private String prompt;

    /** Optional document/book ID to use as RAG context */
    private String bookId;

    /** Chat model to use */
    private String chatModel;

    /** Embedding model for RAG context retrieval */
    private String embeddingModel;

    /** If true, use the document as RAG context alongside the custom prompt */
    @Builder.Default
    private Boolean useDocumentContext = true;
}
