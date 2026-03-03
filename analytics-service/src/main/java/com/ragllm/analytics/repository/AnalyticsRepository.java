package com.ragllm.analytics.repository;

import com.ragllm.common.entity.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsRepository extends JpaRepository<UsageLog, UUID> {

    /**
     * Get total tokens used by tenant in a period.
     */
    @Query("SELECT COALESCE(SUM(u.tokensUsed), 0) FROM UsageLog u " +
           "WHERE u.tenantId = :tenantId AND u.createdAt >= :since AND u.createdAt < :until")
    long sumTokensByTenantInPeriod(@Param("tenantId") UUID tenantId,
                                    @Param("since") Instant since,
                                    @Param("until") Instant until);

    /**
     * Count total queries by tenant in a period.
     */
    @Query("SELECT COUNT(u) FROM UsageLog u " +
           "WHERE u.tenantId = :tenantId AND u.createdAt >= :since AND u.createdAt < :until")
    long countQueriesByTenantInPeriod(@Param("tenantId") UUID tenantId,
                                      @Param("since") Instant since,
                                      @Param("until") Instant until);

    /**
     * Get average response time for a tenant.
     */
    @Query("SELECT COALESCE(AVG(u.responseTimeMs), 0) FROM UsageLog u " +
           "WHERE u.tenantId = :tenantId AND u.createdAt >= :since")
    double avgResponseTimeByTenantSince(@Param("tenantId") UUID tenantId,
                                         @Param("since") Instant since);

    /**
     * Get per-user usage stats for a tenant.
     */
    @Query("SELECT u.userId, COUNT(u), COALESCE(SUM(u.tokensUsed), 0) FROM UsageLog u " +
           "WHERE u.tenantId = :tenantId AND u.createdAt >= :since " +
           "GROUP BY u.userId ORDER BY SUM(u.tokensUsed) DESC")
    List<Object[]> getUserUsageStats(@Param("tenantId") UUID tenantId,
                                      @Param("since") Instant since);

    /**
     * Get daily query counts for a tenant.
     */
    @Query(value = "SELECT DATE(created_at) as day, COUNT(*) as count " +
                   "FROM usage_logs WHERE tenant_id = :tenantId " +
                   "AND created_at >= :since AND created_at < :until " +
                   "GROUP BY DATE(created_at) ORDER BY day",
           nativeQuery = true)
    List<Object[]> getDailyQueryCounts(@Param("tenantId") UUID tenantId,
                                        @Param("since") Instant since,
                                        @Param("until") Instant until);

    /**
     * Get top queried topics/subjects.
     */
    @Query(value = "SELECT LEFT(query, 100), COUNT(*) as freq FROM usage_logs " +
                   "WHERE tenant_id = :tenantId AND created_at >= :since " +
                   "GROUP BY LEFT(query, 100) ORDER BY freq DESC LIMIT 10",
           nativeQuery = true)
    List<Object[]> getTopQueries(@Param("tenantId") UUID tenantId,
                                  @Param("since") Instant since);
}
