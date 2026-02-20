package com.dms.mapper;

import com.dms.domain.Document;
import com.dms.domain.DocumentVersion;
import com.dms.dto.response.DocumentResponse;
import com.dms.dto.response.VersionHistoryResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document document, boolean hasActiveLegalHold) {
        if (document == null) {
            return null;
        }

        return DocumentResponse.builder()
            .id(document.getId())
            .documentTypeName(document.getDocumentType() == null ? null : document.getDocumentType().getName())
            .currentVersion(document.getCurrentVersion())
            .metadata(document.getMetadata())
            .fileSizeBytes(document.getFileSizeBytes())
            .createdAt(document.getCreatedAt())
            .createdBy(document.getCreatedBy())
            .modifiedAt(document.getModifiedAt())
            .modifiedBy(document.getModifiedBy())
            .hasActiveLegalHold(hasActiveLegalHold)
            .retentionExpiresAt(document.getRetentionExpiresAt())
            .build();
    }

    public VersionHistoryResponse toVersionHistoryResponse(DocumentVersion version, UUID documentId) {
        if (version == null) {
            return null;
        }

        return VersionHistoryResponse.builder()
            .documentId(documentId)
            .versionNumber(version.getVersionNumber())
            .blobPath(version.getBlobPath())
            .fileSizeBytes(version.getFileSizeBytes() == null ? 0L : version.getFileSizeBytes())
            .contentHash(version.getContentHash())
            .createdBy(version.getCreatedBy())
            .createdAt(version.getCreatedAt())
            .build();
    }
}
