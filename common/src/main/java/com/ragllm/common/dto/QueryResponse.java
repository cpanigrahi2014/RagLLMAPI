package com.ragllm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    private String answer;
    private List<SourceChunk> sources;
    private int tokensUsed;
    private long responseTimeMs;
    private String chatModel;
    private String embeddingModel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceChunk {
        private String chunkId;
        private String content;
        private String bookName;
        private String chapterTitle;
        private Integer pageNumber;
        private double similarityScore;
    }
}
