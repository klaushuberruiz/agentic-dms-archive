package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.RequirementChunk;
import com.dms.repository.DocumentRepository;
import com.dms.repository.RequirementChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkingServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private RequirementChunkRepository requirementChunkRepository;
    @Mock
    private BlobStorageService blobStorageService;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ChunkingService chunkingService;

    @Test
    void processDocumentAsyncCreatesChunks() {
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        Document document = Document.builder()
            .id(documentId)
            .tenantId(tenantId)
            .blobPath("tenant/doc.pdf")
            .metadata(new HashMap<>())
            .createdAt(Instant.now())
            .build();

        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(documentRepository.findByIdAndTenantId(documentId, tenantId)).thenReturn(Optional.of(document));
        when(blobStorageService.downloadBlob("tenant/doc.pdf")).thenReturn(new ByteArrayInputStream("Sentence one. Sentence two.".getBytes()));

        chunkingService.processDocumentAsync(documentId);

        verify(requirementChunkRepository, atLeastOnce()).save(ArgumentMatchers.any(RequirementChunk.class));
        verify(documentRepository).save(document);
    }
}
