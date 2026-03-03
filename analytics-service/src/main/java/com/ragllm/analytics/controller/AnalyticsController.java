package com.ragllm.analytics.controller;

import com.ragllm.analytics.service.AnalyticsService;
import com.ragllm.common.dto.ApiResponse;
import com.ragllm.common.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get tenant dashboard with overview metrics.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> dashboard = analyticsService.getTenantDashboard(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * Get per-user usage analytics.
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Map<String, Object>> userStats = analyticsService.getUserAnalytics(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(userStats));
    }

    /**
     * Get daily query trend for current month.
     */
    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailyTrend(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Map<String, Object>> trend = analyticsService.getDailyTrend(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    /**
     * Get most popular queries/topics.
     */
    @GetMapping("/top-queries")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopQueries(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Map<String, Object>> topQueries = analyticsService.getTopQueries(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(topQueries));
    }
}
