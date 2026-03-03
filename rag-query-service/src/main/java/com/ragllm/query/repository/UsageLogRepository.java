package com.ragllm.query.repository;

import com.ragllm.common.entity.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, UUID> {

    /**
     * Sum tokens used by a tenant within a time range (monthly quota tracking).
     */
    @Query("SELECT COALESCE(SUM(u.tokensUsed), 0) FROM UsageLog u " +
           "WHERE u.tenantId = :tenantId AND u.createdAt >= :since")
    long sumTokensByTenantSince(@Param("tenantId") UUID tenantId,
                                 @Param("since") Instant since);

    /**
     * Count queries by a tenant within a time range.
     */
    @Query("SELECT COUNT(u) FROM UsageLog u " +
           "WHERE u.tenantId = :tenantId AND u.createdAt >= :since")
    long countQueriesByTenantSince(@Param("tenantId") UUID tenantId,
                                    @Param("since") Instant since);

    /**
     * Sum tokens used by a specific user within a time range.
     */
    @Query("SELECT COALESCE(SUM(u.tokensUsed), 0) FROM UsageLog u " +
           "WHERE u.userId = :userId AND u.createdAt >= :since")
    long sumTokensByUserSince(@Param("userId") UUID userId,
                               @Param("since") Instant since);
}
