package com.ragllm.billing.repository;

import com.ragllm.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Invoice> findByTenantIdAndBillingPeriod(UUID tenantId, String billingPeriod);

    List<Invoice> findAllByStatus(Invoice.InvoiceStatus status);
}
