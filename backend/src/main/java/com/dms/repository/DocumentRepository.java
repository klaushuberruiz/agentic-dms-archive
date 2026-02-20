package com.dms.repository;

import com.dms.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    Page<Document> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);
    
    List<Document> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
    
    Page<Document> findByTenantIdAndDocumentType(UUID tenantId, UUID documentTypeId, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId " +
           "AND d.createdAt BETWEEN :startDate AND :endDate " +
           "AND d.deletedAt IS NULL")
    Page<Document> findByTenantIdAndDateRange(UUID tenantId, Instant startDate, Instant endDate, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId " +
           "AND d.retentionExpiresAt < :now AND d.deletedAt IS NULL")
    List<Document> findExpiredDocuments(UUID tenantId, Instant now);
    
    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId " +
           "AND d.deletedAt IS NOT NULL " +
           "AND d.deletedAt > :recoveryWindowStart")
    List<Document> findSoftDeletedWithinWindow(UUID tenantId, Instant recoveryWindowStart);
}
