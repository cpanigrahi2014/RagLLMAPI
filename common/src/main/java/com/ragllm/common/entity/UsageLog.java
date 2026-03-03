package com.ragllm.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_logs", indexes = {
    @Index(name = "idx_usage_logs_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_usage_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_usage_logs_created_at", columnList = "created_at")
})
@Filter(name = "tenantFilter", condition = "tenant_id = cast(:tenantId as uuid)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tokens_used", nullable = false)
    private Integer tokensUsed;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
