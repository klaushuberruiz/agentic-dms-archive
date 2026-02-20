package com.dms.repository;

import com.dms.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByTenantIdOrderByTimestampDesc(UUID tenantId, Pageable pageable);
    
    Page<AuditLog> findByDocumentIdOrderByTimestampDesc(UUID documentId, Pageable pageable);
    
    Page<AuditLog> findByTenantIdAndUserIdOrderByTimestampDesc(UUID tenantId, String userId, Pageable pageable);
}

