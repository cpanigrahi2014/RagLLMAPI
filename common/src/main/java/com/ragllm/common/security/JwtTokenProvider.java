package com.ragllm.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT utility for token generation and validation.
 * Tokens carry tenant_id, user_id, and role claims.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity:3600000}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity:86400000}") long refreshTokenValidity) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    public String generateAccessToken(UUID userId, UUID tenantId, String role, String email) {
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "tenant_id", tenantId.toString(),
                        "role", role,
                        "email", email
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId, UUID tenantId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("tenant_id", tenantId.toString()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public UUID getTenantId(String token) {
        return UUID.fromString(parseToken(token).get("tenant_id", String.class));
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String getEmail(String token) {
        return parseToken(token).get("email", String.class);
    }
}
