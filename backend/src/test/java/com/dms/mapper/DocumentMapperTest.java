package com.dms.mapper;

import com.dms.domain.Document;
import com.dms.domain.DocumentType;
import com.dms.domain.DocumentVersion;
import com.dms.dto.response.DocumentResponse;
import com.dms.dto.response.VersionHistoryResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMapperTest {

    private final DocumentMapper mapper = new DocumentMapper();

    @Test
    void shouldMapDocumentToResponse() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.now();

        DocumentType documentType = DocumentType.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .name("invoice")
            .retentionDays(365)
            .minRetentionDays(30)
            .active(true)
            .createdAt(now)
            .createdBy("user")
            .entityVersion(0L)
            .build();

        Document document = Document.builder()
            .id(id)
            .tenantId(tenantId)
            .documentType(documentType)
            .currentVersion(2)
            .metadata(Map.of("invoiceNumber", "INV-1"))
            .blobPath("tenant/invoice/2026/01/id_v2.pdf")
            .fileSizeBytes(1024L)
            .contentType("application/pdf")
            .createdAt(now)
            .createdBy("user")
            .retentionExpiresAt(now.plusSeconds(86400))
            .entityVersion(0L)
            .build();

        DocumentResponse response = mapper.toResponse(document, true);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getDocumentTypeName()).isEqualTo("invoice");
        assertThat(response.getCurrentVersion()).isEqualTo(2);
        assertThat(response.isHasActiveLegalHold()).isTrue();
    }

    @Test
    void shouldMapVersionHistoryResponse() {
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.now();
        DocumentVersion version = DocumentVersion.builder()
            .id(UUID.randomUUID())
            .versionNumber(3)
            .blobPath("path")
            .fileSizeBytes(2048L)
            .contentHash("abc")
            .createdBy("user")
            .createdAt(now)
            .build();

        VersionHistoryResponse response = mapper.toVersionHistoryResponse(version, documentId);

        assertThat(response.getDocumentId()).isEqualTo(documentId);
        assertThat(response.getVersionNumber()).isEqualTo(3);
        assertThat(response.getContentHash()).isEqualTo("abc");
    }
}
