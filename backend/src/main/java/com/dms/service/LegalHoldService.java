package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.LegalHold;
import com.dms.dto.request.LegalHoldRequest;
import com.dms.exception.DocumentNotFoundException;
import com.dms.exception.ValidationException;
import com.dms.repository.DocumentRepository;
import com.dms.repository.LegalHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalHoldService {

    private final LegalHoldRepository legalHoldRepository;
    private final DocumentRepository documentRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    @Transactional
    public LegalHold placeLegalHold(LegalHoldRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        Document document = documentRepository.findByIdAndTenantId(request.getDocumentId(), tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        LegalHold legalHold = LegalHold.builder()
            .tenantId(tenantId)
            .document(document)
            .caseReference(request.getCaseReference())
            .reason(request.getReason())
            .placedAt(Instant.now())
            .placedBy(userId)
            .build();

        LegalHold saved = legalHoldRepository.save(legalHold);
        auditService.logMetadataUpdate(document.getId(), Map.of(), Map.of("legalHoldId", saved.getId(), "action", "PLACE"));
        return saved;
    }

    @Transactional
    public LegalHold releaseLegalHold(UUID holdId, String releaseReason) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        LegalHold hold = legalHoldRepository.findByIdAndTenantId(holdId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Legal hold not found"));
        if (hold.getReleasedAt() != null) {
            throw new ValidationException("Legal hold already released");
        }
        hold.setReleasedAt(Instant.now());
        hold.setReleasedBy(userId);
        hold.setReleaseReason(releaseReason);
        LegalHold saved = legalHoldRepository.save(hold);
        auditService.logMetadataUpdate(hold.getDocument().getId(), Map.of(), Map.of("legalHoldId", saved.getId(), "action", "RELEASE"));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LegalHold> getActiveLegalHolds() {
        return legalHoldRepository.findByTenantIdAndReleasedAtIsNull(tenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<LegalHold> getLegalHoldHistory(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        return legalHoldRepository.findByTenantIdAndDocumentIdOrderByPlacedAtDesc(tenantId, documentId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveLegalHolds(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        return !legalHoldRepository.findByTenantIdAndDocumentIdAndReleasedAtIsNull(tenantId, documentId).isEmpty();
    }

    @Transactional(readOnly = true)
    public List<LegalHold> getDocumentsUnderHold(String caseReference) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (caseReference == null || caseReference.isBlank()) {
            return legalHoldRepository.findActiveByTenant(tenantId);
        }
        return legalHoldRepository.findByTenantIdAndCaseReferenceAndReleasedAtIsNull(tenantId, caseReference);
    }

    @Transactional
    public int bulkPlaceHold(List<LegalHoldRequest> requests) {
        int count = 0;
        for (LegalHoldRequest request : requests) {
            placeLegalHold(request);
            count++;
        }
        return count;
    }

    @Transactional
    public int bulkReleaseHold(List<UUID> holdIds, String reason) {
        int count = 0;
        for (UUID holdId : holdIds) {
            releaseLegalHold(holdId, reason);
            count++;
        }
        return count;
    }
}
