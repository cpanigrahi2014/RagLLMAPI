package com.ragllm.auth.service;

import com.ragllm.auth.repository.TenantRepository;
import com.ragllm.auth.repository.UserRepository;
import com.ragllm.common.dto.AuthRequest;
import com.ragllm.common.dto.AuthResponse;
import com.ragllm.common.dto.RegisterRequest;
import com.ragllm.common.entity.Tenant;
import com.ragllm.common.entity.User;
import com.ragllm.common.exception.TenantNotFoundException;
import com.ragllm.common.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Find or create tenant
        Tenant tenant = tenantRepository.findByName(request.getTenantName())
                .orElseGet(() -> createTenant(request.getTenantName()));

        // Check for duplicate email within tenant
        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenant.getId())) {
            throw new IllegalArgumentException("Email already registered for this tenant");
        }

        // Create user
        User user = User.builder()
                .tenantId(tenant.getId())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.valueOf(request.getRole().toUpperCase()))
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("Registered user: {} for tenant: {}", user.getEmail(), tenant.getName());

        return generateAuthResponse(user, tenant);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new BadCredentialsException("Tenant account is suspended");
        }

        log.info("User logged in: {} for tenant: {}", user.getEmail(), tenant.getName());
        return generateAuthResponse(user, tenant);
    }

    private Tenant createTenant(String name) {
        Tenant tenant = Tenant.builder()
                .name(name)
                .subscriptionPlan(Tenant.SubscriptionPlan.FREE)
                .status(Tenant.TenantStatus.ACTIVE)
                .monthlyTokenLimit(50000L) // Free tier: 50K tokens/month
                .build();
        return tenantRepository.save(tenant);
    }

    private AuthResponse generateAuthResponse(User user, Tenant tenant) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), tenant.getId(), user.getRole().name(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), tenant.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .role(user.getRole().name())
                .email(user.getEmail())
                .tenantId(tenant.getId().toString())
                .userId(user.getId().toString())
                .build();
    }
}
