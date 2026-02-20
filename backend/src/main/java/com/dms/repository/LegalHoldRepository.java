package com.dms.repository;

import com.dms.domain.LegalHold;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalHoldRepository extends JpaRepository<LegalHold, UUID> {
    List<LegalHold> findByTenantIdAndReleasedAtIsNull(UUID tenantId);

    List<LegalHold> findByTenantIdAndDocumentIdAndReleasedAtIsNull(UUID tenantId, UUID documentId);

    List<LegalHold> findByTenantIdAndDocumentIdOrderByPlacedAtDesc(UUID tenantId, UUID documentId);

    List<LegalHold> findByTenantIdAndCaseReferenceAndReleasedAtIsNull(UUID tenantId, String caseReference);

    @Query("select l from LegalHold l where l.tenantId = :tenantId and l.releasedAt is null")
    List<LegalHold> findActiveByTenant(UUID tenantId);

    Optional<LegalHold> findByIdAndTenantId(UUID id, UUID tenantId);
}
