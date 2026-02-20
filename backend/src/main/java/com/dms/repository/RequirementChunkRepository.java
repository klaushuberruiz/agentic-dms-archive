package com.dms.repository;

import com.dms.domain.RequirementChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequirementChunkRepository extends JpaRepository<RequirementChunk, UUID> {
    List<RequirementChunk> findByDocumentId(UUID documentId);
    
    List<RequirementChunk> findByTenantId(UUID tenantId);
}
