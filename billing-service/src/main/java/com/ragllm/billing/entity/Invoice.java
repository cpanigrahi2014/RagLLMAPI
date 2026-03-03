package com.ragllm.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "billing_period", nullable = false)
    private String billingPeriod;   // e.g., "2026-03"

    @Column(name = "total_tokens_used", nullable = false)
    private Long totalTokensUsed;

    @Column(name = "total_queries", nullable = false)
    private Long totalQueries;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        currency = "USD";
    }

    public enum InvoiceStatus {
        DRAFT, PENDING, PAID, OVERDUE, CANCELLED
    }
}
