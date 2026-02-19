package com.dms.repository;

import com.dms.domain.LegalHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalHoldRepository extends JpaRepository<LegalHold, UUID> {

    List<LegalHold> findAllByTenantIdAndDocumentIdAndReleasedAtIsNull(UUID tenantId, UUID documentId);

    Optional<LegalHold> findByIdAndTenantId(UUID id, UUID tenantId);
}
