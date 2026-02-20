package com.dms.repository;

import com.dms.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByTenantIdOrderByTimestampDesc(UUID tenantId, Pageable pageable);
    
    Page<AuditLog> findByEntityIdOrderByTimestampDesc(UUID entityId, Pageable pageable);
    
    Page<AuditLog> findByTenantIdAndUserIdOrderByTimestampDesc(UUID tenantId, String userId, Pageable pageable);

    Optional<AuditLog> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantIdAndTimestampBetween(UUID tenantId, Instant start, Instant end);

    long countByTenantIdAndActionAndTimestampBetween(UUID tenantId, String action, Instant start, Instant end);
}

