package com.dms.repository;

import com.dms.domain.Document;
import com.dms.domain.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
class DocumentRepositoryIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @Test
    void shouldFilterByTenantId() {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        DocumentType type1 = createDocumentType(tenant1, "invoice");
        DocumentType type2 = createDocumentType(tenant2, "invoice");
        createDocument(tenant1, type1, Map.of("invoiceNumber", "INV-100"), null);
        createDocument(tenant2, type2, Map.of("invoiceNumber", "INV-200"), null);

        var page = documentRepository.findByTenantIdAndDeletedAtIsNull(tenant1, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTenantId()).isEqualTo(tenant1);
    }

    @Test
    void shouldQueryMetadataField() {
        UUID tenantId = UUID.randomUUID();
        DocumentType type = createDocumentType(tenantId, "invoice");
        createDocument(tenantId, type, Map.of("invoiceNumber", "INV-100"), null);
        createDocument(tenantId, type, Map.of("invoiceNumber", "INV-200"), null);

        var results = documentRepository.findByMetadataField(tenantId, "invoiceNumber", "INV-100");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMetadata()).containsEntry("invoiceNumber", "INV-100");
    }

    @Test
    void shouldExcludeSoftDeleted() {
        UUID tenantId = UUID.randomUUID();
        DocumentType type = createDocumentType(tenantId, "invoice");
        createDocument(tenantId, type, Map.of("invoiceNumber", "INV-100"), null);
        createDocument(tenantId, type, Map.of("invoiceNumber", "INV-200"), Instant.now());

        var page = documentRepository.findByTenantIdAndDeletedAtIsNull(tenantId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getMetadata()).containsEntry("invoiceNumber", "INV-100");
    }

    @Test
    void shouldPaginateResults() {
        UUID tenantId = UUID.randomUUID();
        DocumentType type = createDocumentType(tenantId, "invoice");
        for (int i = 0; i < 5; i++) {
            createDocument(tenantId, type, Map.of("invoiceNumber", "INV-" + i), null);
        }

        var page = documentRepository.findByTenantIdAndDeletedAtIsNull(tenantId, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    void shouldSearchFullText() {
        UUID tenantId = UUID.randomUUID();
        DocumentType type = createDocumentType(tenantId, "invoice");
        createDocument(tenantId, type, Map.of("description", "alpha billing"), null);
        createDocument(tenantId, type, Map.of("description", "beta receipts"), null);

        var page = documentRepository.searchFullText(tenantId, "alpha", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getMetadata().get("description")).isEqualTo("alpha billing");
    }

    private DocumentType createDocumentType(UUID tenantId, String name) {
        DocumentType type = DocumentType.builder()
            .tenantId(tenantId)
            .name(name + "-" + UUID.randomUUID())
            .displayName(name)
            .description(name + " docs")
            .metadataSchema(Map.of())
            .allowedGroups(new UUID[0])
            .retentionDays(365)
            .minRetentionDays(0)
            .active(true)
            .createdAt(Instant.now())
            .createdBy("test")
            .entityVersion(0L)
            .build();
        return documentTypeRepository.save(type);
    }

    private void createDocument(UUID tenantId, DocumentType type, Map<String, Object> metadata, Instant deletedAt) {
        Document document = Document.builder()
            .tenantId(tenantId)
            .documentType(type)
            .currentVersion(1)
            .metadata(metadata)
            .blobPath("blob/" + UUID.randomUUID() + ".pdf")
            .fileSizeBytes(123L)
            .contentType("application/pdf")
            .contentHash("hash")
            .createdAt(Instant.now())
            .createdBy("test")
            .deletedAt(deletedAt)
            .retentionExpiresAt(Instant.now().plusSeconds(86400))
            .entityVersion(0L)
            .build();
        documentRepository.save(document);
    }
}
