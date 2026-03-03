package com.ragllm.common.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Represents the authenticated user principal extracted from JWT.
 */
@Getter
@AllArgsConstructor
public class UserPrincipal {
    private final UUID userId;
    private final UUID tenantId;
    private final String email;
    private final String role;
}
