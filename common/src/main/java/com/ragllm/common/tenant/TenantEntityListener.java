package com.ragllm.common.tenant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * JPA Entity Listener ensuring tenant_id is always set on persist and
 * validated on update for tenant-scoped entities.
 */
@Component
public class TenantEntityListener {

    @PrePersist
    public void setTenantOnCreate(Object entity) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return;
        }
        setTenantIdField(entity, tenantId);
    }

    @PreUpdate
    public void validateTenantOnUpdate(Object entity) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return;
        }
        UUID entityTenantId = getTenantIdField(entity);
        if (entityTenantId != null && !entityTenantId.equals(tenantId)) {
            throw new SecurityException("Cross-tenant data modification attempt detected");
        }
    }

    private void setTenantIdField(Object entity, UUID tenantId) {
        try {
            Field field = findField(entity.getClass(), "tenantId");
            if (field != null) {
                field.setAccessible(true);
                if (field.get(entity) == null) {
                    field.set(entity, tenantId);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set tenant_id on entity", e);
        }
    }

    private UUID getTenantIdField(Object entity) {
        try {
            Field field = findField(entity.getClass(), "tenantId");
            if (field != null) {
                field.setAccessible(true);
                return (UUID) field.get(entity);
            }
            return null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to read tenant_id from entity", e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
