package com.dms.service;

import com.dms.domain.DocumentType;
import com.dms.repository.DocumentTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTypeServiceTest {

    @Mock
    private DocumentTypeRepository repository;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private AuditService auditService;
    @InjectMocks
    private DocumentTypeService service;

    @Test
    void shouldCreateDocumentType_whenValid() {
        UUID tenantId = UUID.randomUUID();
        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantContext.getCurrentUserId()).thenReturn("alice");
        when(repository.findByNameAndTenantId("invoice", tenantId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentType type = DocumentType.builder()
            .name("invoice")
            .metadataSchema(Map.of("type", "object"))
            .build();

        DocumentType result = service.createDocumentType(type);
        assertEquals("invoice", result.getName());
        assertEquals(tenantId, result.getTenantId());
    }

    @Test
    void shouldRejectDuplicateName_whenCreatingDocumentType() {
        UUID tenantId = UUID.randomUUID();
        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(repository.findByNameAndTenantId("invoice", tenantId)).thenReturn(Optional.of(DocumentType.builder().build()));

        DocumentType type = DocumentType.builder().name("invoice").build();
        assertThrows(RuntimeException.class, () -> service.createDocumentType(type));
    }
}

