package com.ragllm.common.tenant;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AOP aspect that enables Hibernate tenant filter before any repository operation.
 * Ensures all queries are scoped to the current tenant.
 */
@Aspect
@Component
public class TenantFilterAspect {

    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* com.ragllm..repository..*(..))")
    public void enableTenantFilter() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("tenantId", tenantId.toString());
        }
    }
}
