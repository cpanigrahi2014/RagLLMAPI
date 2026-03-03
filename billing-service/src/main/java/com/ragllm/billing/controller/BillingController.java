package com.ragllm.billing.controller;

import com.ragllm.billing.entity.Invoice;
import com.ragllm.billing.service.BillingService;
import com.ragllm.common.dto.ApiResponse;
import com.ragllm.common.entity.Tenant;
import com.ragllm.common.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Get all invoices for the current tenant.
     */
    @GetMapping("/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Invoice>>> getInvoices(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Invoice> invoices = billingService.getInvoices(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    /**
     * Get estimated cost for the current billing period.
     */
    @GetMapping("/current-cost")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentCost(
            @AuthenticationPrincipal UserPrincipal principal) {
        BigDecimal cost = billingService.calculateCurrentPeriodCost(principal.getTenantId());
        Map<String, Object> result = Map.of(
                "estimatedCost", cost,
                "currency", "USD",
                "tenantId", principal.getTenantId().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Upgrade subscription plan.
     */
    @PostMapping("/upgrade")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> upgradePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {

        String planName = request.get("plan");
        Tenant.SubscriptionPlan plan = Tenant.SubscriptionPlan.valueOf(planName.toUpperCase());

        Tenant tenant = billingService.upgradePlan(principal.getTenantId(), plan);
        Map<String, String> result = Map.of(
                "plan", tenant.getSubscriptionPlan().name(),
                "monthlyTokenLimit", String.valueOf(tenant.getMonthlyTokenLimit()),
                "message", "Plan upgraded successfully"
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
