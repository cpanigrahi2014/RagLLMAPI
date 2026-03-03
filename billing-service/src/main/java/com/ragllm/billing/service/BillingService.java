package com.ragllm.billing.service;

import com.ragllm.billing.entity.Invoice;
import com.ragllm.billing.repository.InvoiceRepository;
import com.ragllm.billing.repository.TenantRepository;
import com.ragllm.billing.repository.UsageLogRepository;
import com.ragllm.common.entity.Tenant;
import com.ragllm.common.exception.TenantNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages subscription plans, invoice generation, and cost calculations.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    // Pricing per plan (monthly USD)
    private static final Map<Tenant.SubscriptionPlan, PlanConfig> PLAN_CONFIGS = Map.of(
            Tenant.SubscriptionPlan.FREE, new PlanConfig(0.00, 50_000L),
            Tenant.SubscriptionPlan.BASIC, new PlanConfig(29.99, 500_000L),
            Tenant.SubscriptionPlan.STANDARD, new PlanConfig(99.99, 2_000_000L),
            Tenant.SubscriptionPlan.PREMIUM, new PlanConfig(299.99, 10_000_000L),
            Tenant.SubscriptionPlan.ENTERPRISE, new PlanConfig(999.99, 50_000_000L)
    );

    // Cost per 1K tokens for overage
    private static final double OVERAGE_COST_PER_1K = 0.002;

    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final UsageLogRepository usageLogRepository;

    public BillingService(InvoiceRepository invoiceRepository,
                          TenantRepository tenantRepository,
                          UsageLogRepository usageLogRepository) {
        this.invoiceRepository = invoiceRepository;
        this.tenantRepository = tenantRepository;
        this.usageLogRepository = usageLogRepository;
    }

    /**
     * Upgrade a tenant's subscription plan.
     */
    @Transactional
    public Tenant upgradePlan(UUID tenantId, Tenant.SubscriptionPlan newPlan) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        PlanConfig config = PLAN_CONFIGS.get(newPlan);
        tenant.setSubscriptionPlan(newPlan);
        tenant.setMonthlyTokenLimit(config.tokenLimit());

        log.info("Tenant {} upgraded to {} plan", tenantId, newPlan);
        return tenantRepository.save(tenant);
    }

    /**
     * Get invoices for a tenant.
     */
    @Transactional(readOnly = true)
    public List<Invoice> getInvoices(UUID tenantId) {
        return invoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    /**
     * Calculate estimated cost for current billing period.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCurrentPeriodCost(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        PlanConfig config = PLAN_CONFIGS.get(tenant.getSubscriptionPlan());
        BigDecimal baseCost = BigDecimal.valueOf(config.monthlyPrice());

        // Calculate overage
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        Instant monthStart = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long tokensUsed = usageLogRepository.sumTokensByTenantInPeriod(tenantId, monthStart, monthEnd);
        long overageTokens = Math.max(0, tokensUsed - config.tokenLimit());

        BigDecimal overageCost = BigDecimal.ZERO;
        if (overageTokens > 0) {
            overageCost = BigDecimal.valueOf(overageTokens / 1000.0 * OVERAGE_COST_PER_1K)
                    .setScale(4, RoundingMode.HALF_UP);
        }

        return baseCost.add(overageCost).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate monthly invoices for all active tenants.
     * Runs on the 1st of each month at 00:05 UTC.
     */
    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void generateMonthlyInvoices() {
        log.info("Starting monthly invoice generation");

        YearMonth previousMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
        String billingPeriod = previousMonth.toString();
        Instant periodStart = previousMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant periodEnd = previousMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Tenant> activeTenants = tenantRepository.findAll().stream()
                .filter(t -> t.getStatus() == Tenant.TenantStatus.ACTIVE)
                .toList();

        for (Tenant tenant : activeTenants) {
            try {
                // Skip if invoice already exists
                if (invoiceRepository.findByTenantIdAndBillingPeriod(tenant.getId(), billingPeriod).isPresent()) {
                    continue;
                }

                long tokensUsed = usageLogRepository.sumTokensByTenantInPeriod(
                        tenant.getId(), periodStart, periodEnd);
                long queries = usageLogRepository.countQueriesByTenantInPeriod(
                        tenant.getId(), periodStart, periodEnd);

                PlanConfig config = PLAN_CONFIGS.get(tenant.getSubscriptionPlan());
                BigDecimal baseCost = BigDecimal.valueOf(config.monthlyPrice());

                long overageTokens = Math.max(0, tokensUsed - config.tokenLimit());
                BigDecimal overageCost = BigDecimal.valueOf(overageTokens / 1000.0 * OVERAGE_COST_PER_1K)
                        .setScale(4, RoundingMode.HALF_UP);

                BigDecimal total = baseCost.add(overageCost).setScale(2, RoundingMode.HALF_UP);

                Invoice invoice = Invoice.builder()
                        .tenantId(tenant.getId())
                        .billingPeriod(billingPeriod)
                        .totalTokensUsed(tokensUsed)
                        .totalQueries(queries)
                        .amount(total)
                        .status(total.compareTo(BigDecimal.ZERO) > 0
                                ? Invoice.InvoiceStatus.PENDING : Invoice.InvoiceStatus.PAID)
                        .build();

                invoiceRepository.save(invoice);
                log.info("Generated invoice for tenant {}: ${} for period {}", 
                         tenant.getId(), total, billingPeriod);

            } catch (Exception e) {
                log.error("Failed to generate invoice for tenant {}", tenant.getId(), e);
            }
        }

        log.info("Monthly invoice generation completed for {} tenants", activeTenants.size());
    }

    private record PlanConfig(double monthlyPrice, long tokenLimit) {}
}
