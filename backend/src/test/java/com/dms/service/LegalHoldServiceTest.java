package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.LegalHold;
import com.dms.dto.request.LegalHoldRequest;
import com.dms.repository.DocumentRepository;
import com.dms.repository.LegalHoldRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalHoldServiceTest {

    @Mock
    private LegalHoldRepository legalHoldRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private AuditService auditService;
    @InjectMocks
    private LegalHoldService legalHoldService;

    @Test
    void shouldPlaceLegalHold_whenDocumentExists() {
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantContext.getCurrentUserId()).thenReturn("legal");
        when(documentRepository.findByIdAndTenantId(documentId, tenantId)).thenReturn(Optional.of(Document.builder().id(documentId).build()));
        when(legalHoldRepository.save(any())).thenAnswer(invocation -> {
            LegalHold hold = invocation.getArgument(0);
            hold.setId(UUID.randomUUID());
            return hold;
        });

        LegalHoldRequest request = LegalHoldRequest.builder().documentId(documentId).caseReference("CASE-1").reason("investigation").build();
        LegalHold hold = legalHoldService.placeLegalHold(request);
        assertTrue(hold.getCaseReference().startsWith("CASE"));
    }

    @Test
    void shouldReturnFalse_whenNoActiveHolds() {
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(legalHoldRepository.findByTenantIdAndDocumentIdAndReleasedAtIsNull(tenantId, documentId)).thenReturn(List.of());
        assertFalse(legalHoldService.hasActiveLegalHolds(documentId));
    }
}
