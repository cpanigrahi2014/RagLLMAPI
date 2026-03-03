package com.ragllm.query.service;

import com.ragllm.common.entity.Tenant;
import com.ragllm.common.exception.QuotaExceededException;
import com.ragllm.common.exception.TenantNotFoundException;
import com.ragllm.common.entity.UsageLog;
import com.ragllm.query.repository.TenantRepository;
import com.ragllm.query.repository.UsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Tracks token usage per tenant and enforces monthly quotas.
 */
@Service
public class UsageTrackingService {

    private static final Logger log = LoggerFactory.getLogger(UsageTrackingService.class);

    // Cost per 1K tokens (approximate for GPT-4.1-mini)
    private static final double COST_PER_1K_INPUT_TOKENS = 0.00015;
    private static final double COST_PER_1K_OUTPUT_TOKENS = 0.0006;

    private final UsageLogRepository usageLogRepository;
    private final TenantRepository tenantRepository;

    public UsageTrackingService(UsageLogRepository usageLogRepository,
                                 TenantRepository tenantRepository) {
        this.usageLogRepository = usageLogRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Check if tenant has remaining quota for a query.
     */
    public void enforceQuota(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new QuotaExceededException("Tenant account is not active");
        }

        Instant monthStart = getMonthStart();
        long tokensUsed = usageLogRepository.sumTokensByTenantSince(tenantId, monthStart);

        if (tokensUsed >= tenant.getMonthlyTokenLimit()) {
            log.warn("Tenant {} exceeded monthly quota: {}/{}", tenantId, tokensUsed, tenant.getMonthlyTokenLimit());
            throw new QuotaExceededException(
                    String.format("Monthly token quota exceeded. Used: %d, Limit: %d",
                            tokensUsed, tenant.getMonthlyTokenLimit()));
        }
    }

    /**
     * Log token usage for a query.
     */
    @Transactional
    @CacheEvict(value = "usageCache", key = "#tenantId")
    public void logUsage(UUID tenantId, UUID userId, String query,
                          int promptTokens, int completionTokens,
                          String model, long responseTimeMs) {
        UsageLog usageLog = UsageLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .query(query)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .tokensUsed(promptTokens + completionTokens)
                .modelUsed(model)
                .responseTimeMs(responseTimeMs)
                .build();

        usageLogRepository.save(usageLog);
        log.debug("Logged usage: {} tokens for tenant {} user {}", 
                   promptTokens + completionTokens, tenantId, userId);
    }

    /**
     * Get current month's usage for a tenant.
     */
    @Cacheable(value = "usageCache", key = "#tenantId")
    public long getCurrentMonthUsage(UUID tenantId) {
        return usageLogRepository.sumTokensByTenantSince(tenantId, getMonthStart());
    }

    /**
     * Get remaining tokens for the current month.
     */
    public long getRemainingTokens(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
        long used = getCurrentMonthUsage(tenantId);
        return Math.max(0, tenant.getMonthlyTokenLimit() - used);
    }

    /**
     * Calculate estimated cost in USD for a tenant's monthly usage.
     */
    public double calculateMonthlyCost(UUID tenantId) {
        Instant monthStart = getMonthStart();

        // Sum prompt and completion tokens separately for accurate cost
        long totalTokens = usageLogRepository.sumTokensByTenantSince(tenantId, monthStart);

        // Approximate split: 70% input, 30% output
        double inputCost = (totalTokens * 0.7 / 1000.0) * COST_PER_1K_INPUT_TOKENS;
        double outputCost = (totalTokens * 0.3 / 1000.0) * COST_PER_1K_OUTPUT_TOKENS;

        return Math.round((inputCost + outputCost) * 10000.0) / 10000.0;
    }

    private Instant getMonthStart() {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        return currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
