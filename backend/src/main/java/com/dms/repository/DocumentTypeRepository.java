package com.dms.repository;

import com.dms.domain.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, UUID> {
    Optional<DocumentType> findByIdAndTenantId(UUID id, UUID tenantId);
    
    Optional<DocumentType> findByNameAndTenantId(String name, UUID tenantId);
    
    Page<DocumentType> findByTenantIdAndIsActive(UUID tenantId, boolean isActive, Pageable pageable);
    
    List<DocumentType> findByTenantIdAndIsActive(UUID tenantId, boolean isActive);
}

