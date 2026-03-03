package com.ragllm.billing.repository;

import com.ragllm.common.entity.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, UUID> {

    @Query("SELECT COALESCE(SUM(u.tokensUsed), 0) FROM UsageLog u " +
           "WHERE u.tenantId = :tenantId AND u.createdAt >= :since AND u.createdAt < :until")
    long sumTokensByTenantInPeriod(@Param("tenantId") UUID tenantId,
                                    @Param("since") Instant since,
                                    @Param("until") Instant until);

    @Query("SELECT COUNT(u) FROM UsageLog u " +
           "WHERE u.tenantId = :tenantId AND u.createdAt >= :since AND u.createdAt < :until")
    long countQueriesByTenantInPeriod(@Param("tenantId") UUID tenantId,
                                      @Param("since") Instant since,
                                      @Param("until") Instant until);
}
