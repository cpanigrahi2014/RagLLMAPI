package com.ragllm.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "subscription_plan", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan subscriptionPlan;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TenantStatus status;

    @Column(name = "monthly_token_limit", nullable = false)
    private Long monthlyTokenLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum SubscriptionPlan {
        FREE, BASIC, STANDARD, PREMIUM, ENTERPRISE
    }

    public enum TenantStatus {
        ACTIVE, SUSPENDED, CANCELLED
    }
}
