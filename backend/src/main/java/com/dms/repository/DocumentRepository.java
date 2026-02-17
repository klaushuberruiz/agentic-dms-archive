package com.dms.repository;

import com.dms.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    Optional<Document> findByIdAndTenantId(UUID id, UUID tenantId);
    
    List<Document> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
    
    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId " +
           "AND d.retentionExpiresAt < :now AND d.deletedAt IS NULL")
    List<Document> findExpiredDocuments(UUID tenantId, Instant now);
}
