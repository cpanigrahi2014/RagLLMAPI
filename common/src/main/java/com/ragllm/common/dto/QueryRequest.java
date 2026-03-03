package com.ragllm.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    @NotBlank(message = "Query is required")
    @Size(max = 2000, message = "Query must not exceed 2000 characters")
    private String query;

    private String subject;
    private Integer classLevel;
    private Integer maxResults;

    /** Chat model to use (e.g. gpt-4.1-mini, gpt-4o, gpt-4.1). Null = default. */
    private String chatModel;

    /** Embedding model to use (e.g. text-embedding-3-small, text-embedding-3-large). Null = default. */
    private String embeddingModel;

    /** If false, skip RAG (vector search) and call LLM directly without document context. Default = true. */
    @Builder.Default
    private Boolean useRag = true;
}
