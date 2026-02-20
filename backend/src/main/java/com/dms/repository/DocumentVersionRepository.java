package com.dms.repository;

import com.dms.domain.Document;
import com.dms.domain.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    List<DocumentVersion> findAllByDocumentOrderByVersionNumberDesc(Document document);
    Optional<DocumentVersion> findByDocumentAndVersionNumber(Document document, Integer versionNumber);
}
