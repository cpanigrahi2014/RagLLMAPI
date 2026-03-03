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
public class StudioResponse {

    private String result;
    private int tokensUsed;
    private long responseTimeMs;
    private String chatModel;
    private String type;

    /** For Q&A generation — structured list of question-answer pairs */
    private List<QAPair> qaPairs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAPair {
        private int number;
        private String question;
        private String answer;
        private String difficulty;
    }
}
