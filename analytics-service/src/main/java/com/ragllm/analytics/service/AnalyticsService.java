package com.ragllm.analytics.service;

import com.ragllm.analytics.repository.AnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    /**
     * Get comprehensive tenant dashboard analytics.
     */
    public Map<String, Object> getTenantDashboard(UUID tenantId) {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        Instant monthStart = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long totalTokens = analyticsRepository.sumTokensByTenantInPeriod(tenantId, monthStart, monthEnd);
        long totalQueries = analyticsRepository.countQueriesByTenantInPeriod(tenantId, monthStart, monthEnd);
        double avgResponseTime = analyticsRepository.avgResponseTimeByTenantSince(tenantId, monthStart);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("period", currentMonth.toString());
        dashboard.put("totalTokensUsed", totalTokens);
        dashboard.put("totalQueries", totalQueries);
        dashboard.put("averageResponseTimeMs", Math.round(avgResponseTime));
        dashboard.put("averageTokensPerQuery", totalQueries > 0 ? totalTokens / totalQueries : 0);

        return dashboard;
    }

    /**
     * Get per-user usage breakdown.
     */
    public List<Map<String, Object>> getUserAnalytics(UUID tenantId) {
        Instant monthStart = YearMonth.now(ZoneOffset.UTC)
                .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> userStats = analyticsRepository.getUserUsageStats(tenantId, monthStart);

        return userStats.stream().map(row -> {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("userId", row[0].toString());
            stat.put("queryCount", ((Number) row[1]).longValue());
            stat.put("tokensUsed", ((Number) row[2]).longValue());
            return stat;
        }).toList();
    }

    /**
     * Get daily query trend for the current month.
     */
    public List<Map<String, Object>> getDailyTrend(UUID tenantId) {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        Instant monthStart = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> dailyCounts = analyticsRepository.getDailyQueryCounts(
                tenantId, monthStart, monthEnd);

        return dailyCounts.stream().map(row -> {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", row[0].toString());
            day.put("queryCount", ((Number) row[1]).longValue());
            return day;
        }).toList();
    }

    /**
     * Get most popular queries/topics.
     */
    public List<Map<String, Object>> getTopQueries(UUID tenantId) {
        Instant monthStart = YearMonth.now(ZoneOffset.UTC)
                .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> topQueries = analyticsRepository.getTopQueries(tenantId, monthStart);

        return topQueries.stream().map(row -> {
            Map<String, Object> topic = new LinkedHashMap<>();
            topic.put("query", row[0]);
            topic.put("frequency", ((Number) row[1]).longValue());
            return topic;
        }).toList();
    }
}
