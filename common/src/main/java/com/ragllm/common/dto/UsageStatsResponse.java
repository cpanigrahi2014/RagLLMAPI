package com.ragllm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatsResponse {
    private String tenantId;
    private String tenantName;
    private String subscriptionPlan;
    private long totalTokensUsed;
    private long monthlyTokenLimit;
    private long remainingTokens;
    private double usagePercentage;
    private long totalQueries;
    private double estimatedCostUsd;
}
