package com.ragllm.document.repository;

import com.ragllm.common.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    List<Book> findAllByTenantId(UUID tenantId);

    Optional<Book> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Book> findAllByTenantIdAndSubject(UUID tenantId, String subject);

    List<Book> findAllByTenantIdAndClassLevel(UUID tenantId, Integer classLevel);

    long countByTenantId(UUID tenantId);
}
