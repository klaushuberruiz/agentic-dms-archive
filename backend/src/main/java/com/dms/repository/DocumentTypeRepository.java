package com.dms.repository;

import com.dms.domain.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, UUID> {
    Optional<DocumentType> findByIdAndTenantId(UUID id, UUID tenantId);
}
