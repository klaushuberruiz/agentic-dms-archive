package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.DocumentType;
import com.dms.repository.DocumentRepository;
import com.dms.repository.DocumentVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersioningServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentVersionRepository documentVersionRepository;
    @Mock
    private BlobStorageService blobStorageService;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private AuditService auditService;
    @InjectMocks
    private VersioningService versioningService;

    @Test
    void shouldUploadNewVersion_whenDocumentExists() {
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentType documentType = DocumentType.builder().name("invoice").build();
        Document document = Document.builder()
            .id(documentId)
            .tenantId(tenantId)
            .documentType(documentType)
            .currentVersion(1)
            .build();

        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantContext.getCurrentUserId()).thenReturn("alice");
        when(documentRepository.findByIdAndTenantId(documentId, tenantId)).thenReturn(Optional.of(document));
        MockMultipartFile file = new MockMultipartFile("file", "v2.pdf", "application/pdf", "%PDF-test".getBytes());

        versioningService.uploadNewVersion(documentId, file);

        verify(documentVersionRepository, times(1)).save(any());
        verify(documentRepository, times(1)).save(any());
    }
}

