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

    Optional<Document> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Page<Document> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    @Query(
        value = """
            SELECT *
            FROM documents d
            WHERE d.tenant_id = :tenantId
              AND d.deleted_at IS NULL
              AND d.metadata ->> :fieldName = :fieldValue
            """,
        nativeQuery = true
    )
    Page<Document> findByMetadataField(UUID tenantId, String fieldName, String fieldValue, Pageable pageable);

    @Query(
        value = """
            SELECT *
            FROM documents d
            WHERE d.tenant_id = :tenantId
              AND d.deleted_at IS NULL
              AND d.metadata_tsv @@ plainto_tsquery('english', :searchTerm)
            """,
        nativeQuery = true
    )
    Page<Document> searchByMetadataTsv(UUID tenantId, String searchTerm, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.retentionExpiresAt < :now AND d.deletedAt IS NULL")
    List<Document> findExpiredDocuments(UUID tenantId, Instant now);
}
