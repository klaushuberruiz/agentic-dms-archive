package com.dms.repository;

import com.dms.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByTenantIdAndEntityIdAndTimestampBetween(
        UUID tenantId,
        UUID entityId,
        Instant from,
        Instant to,
        Pageable pageable
    );

    Page<AuditLog> findAllByTenantIdAndUserIdAndTimestampBetween(
        UUID tenantId,
        String userId,
        Instant from,
        Instant to,
        Pageable pageable
    );

    Page<AuditLog> findAllByTenantIdAndActionAndTimestampBetween(
        UUID tenantId,
        String action,
        Instant from,
        Instant to,
        Pageable pageable
    );
}
