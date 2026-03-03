package com.ragllm.query.repository;

import com.ragllm.common.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByIdAndTenantId(UUID id, UUID tenantId);
}
