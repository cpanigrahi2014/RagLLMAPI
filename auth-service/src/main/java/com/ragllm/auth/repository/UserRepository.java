package com.ragllm.auth.repository;

import com.ragllm.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<User> findByEmail(String email);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    List<User> findAllByTenantId(UUID tenantId);

    long countByTenantId(UUID tenantId);
}
