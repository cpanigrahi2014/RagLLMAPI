package com.ragllm.query.controller;

import com.ragllm.common.dto.ApiResponse;
import com.ragllm.common.dto.QueryRequest;
import com.ragllm.common.dto.QueryResponse;
import com.ragllm.common.dto.UsageStatsResponse;
import com.ragllm.common.entity.Tenant;
import com.ragllm.common.exception.TenantNotFoundException;
import com.ragllm.common.security.UserPrincipal;
import com.ragllm.query.repository.TenantRepository;
import com.ragllm.query.service.RagQueryService;
import com.ragllm.query.service.UsageTrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {

    private final RagQueryService ragQueryService;
    private final UsageTrackingService usageTrackingService;
    private final TenantRepository tenantRepository;

    public QueryController(RagQueryService ragQueryService,
                            UsageTrackingService usageTrackingService,
                            TenantRepository tenantRepository) {
        this.ragQueryService = ragQueryService;
        this.usageTrackingService = usageTrackingService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Submit a RAG query.
     * The query is processed through the full RAG pipeline.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QueryResponse>> query(
            @Valid @RequestBody QueryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        QueryResponse response = ragQueryService.queryCached(request, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get current usage statistics for the authenticated tenant.
     */
    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<UsageStatsResponse>> getUsage(
            @AuthenticationPrincipal UserPrincipal principal) {

        Tenant tenant = tenantRepository.findById(principal.getTenantId())
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        long tokensUsed = usageTrackingService.getCurrentMonthUsage(principal.getTenantId());
        long remaining = usageTrackingService.getRemainingTokens(principal.getTenantId());
        double cost = usageTrackingService.calculateMonthlyCost(principal.getTenantId());

        UsageStatsResponse stats = UsageStatsResponse.builder()
                .tenantId(tenant.getId().toString())
                .tenantName(tenant.getName())
                .subscriptionPlan(tenant.getSubscriptionPlan().name())
                .totalTokensUsed(tokensUsed)
                .monthlyTokenLimit(tenant.getMonthlyTokenLimit())
                .remainingTokens(remaining)
                .usagePercentage(tenant.getMonthlyTokenLimit() > 0
                        ? (double) tokensUsed / tenant.getMonthlyTokenLimit() * 100 : 0)
                .estimatedCostUsd(cost)
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
