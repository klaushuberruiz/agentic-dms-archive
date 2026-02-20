package com.dms.service;

import com.dms.domain.Document;
import com.dms.repository.DocumentRepository;
import com.dms.repository.RequirementChunkRepository;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

class ChunkingServiceProperties {

    @Property
    void randomTextDoesNotThrow(@ForAll String text) {
        DocumentRepository documentRepository = Mockito.mock(DocumentRepository.class);
        RequirementChunkRepository chunkRepository = Mockito.mock(RequirementChunkRepository.class);
        BlobStorageService blobStorageService = Mockito.mock(BlobStorageService.class);
        TenantContext tenantContext = Mockito.mock(TenantContext.class);
        AuditService auditService = Mockito.mock(AuditService.class);

        ChunkingService service = new ChunkingService(documentRepository, chunkRepository, blobStorageService, tenantContext, auditService);

        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Document document = Document.builder()
            .id(documentId)
            .tenantId(tenantId)
            .blobPath("tenant/doc.txt")
            .metadata(new HashMap<>())
            .createdAt(Instant.now())
            .build();

        Mockito.when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        Mockito.when(documentRepository.findByIdAndTenantId(documentId, tenantId)).thenReturn(Optional.of(document));
        Mockito.when(blobStorageService.downloadBlob("tenant/doc.txt")).thenReturn(new ByteArrayInputStream(text.getBytes()));

        service.processDocumentAsync(documentId);
    }
}
