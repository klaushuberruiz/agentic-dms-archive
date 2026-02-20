package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.DocumentType;
import com.dms.dto.response.RetentionStatusResponse;
import com.dms.exception.DocumentNotFoundException;
import com.dms.exception.ValidationException;
import com.dms.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionService {
    
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final LegalHoldService legalHoldService;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    
    @Transactional
    public void markForDeletion(UUID documentId, Integer retentionDays) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        
        // Validate document
        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        
        if (document.isDeleted()) {
            throw new ValidationException("Cannot set retention on deleted document");
        }
        
        // Validate retention days
        if (retentionDays == null || retentionDays < 0) {
            throw new ValidationException("Retention days must be non-negative");
        }
        if (retentionDays > 36500) { // ~100 years
            throw new ValidationException("Retention days cannot exceed 36500 (100 years)");
        }
        
        // Calculate expiry
        Instant expiresAt = Instant.now().plus(retentionDays, ChronoUnit.DAYS);
        
        // Update retention expiry
        document.setRetentionExpiresAt(expiresAt);
        documentRepository.save(document);
        
        log.info("Document marked for retention: documentId={}, expiresAt={}, days={}", 
            documentId, expiresAt, retentionDays);
    }
    
    @Transactional
    public void setRetentionFromDocumentType(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        
        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        
        DocumentType docType = document.getDocumentType();
        if (docType.getDefaultRetentionDays() != null && docType.getDefaultRetentionDays() > 0) {
            markForDeletion(documentId, docType.getDefaultRetentionDays());
        }
    }
    
    @Transactional(readOnly = true)
    public RetentionStatusResponse getDocumentRetentionStatus(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        
        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        
        Instant now = Instant.now();
        Instant retentionExpiresAt = document.getRetentionExpiresAt();
        
        boolean hasActiveLegalHolds = legalHoldService.hasActiveLegalHolds(documentId);
        boolean isEligibleForHardDelete = retentionExpiresAt != null 
            && now.isAfter(retentionExpiresAt) 
            && !hasActiveLegalHolds;
        
        Long daysUntilRetention = null;
        if (retentionExpiresAt != null) {
            daysUntilRetention = ChronoUnit.DAYS.between(now, retentionExpiresAt);
        }
        
        return RetentionStatusResponse.builder()
            .documentId(documentId)
            .documentType(document.getDocumentType().getName())
            .defaultRetentionDays(document.getDocumentType().getDefaultRetentionDays())
            .retentionExpiresAt(retentionExpiresAt)
            .daysUntilRetention(daysUntilRetention)
            .hasActiveLegalHolds(hasActiveLegalHolds)
            .isEligibleForHardDelete(isEligibleForHardDelete)
            .isSoftDeleted(document.isDeleted())
            .build();
    }
    
    @Transactional
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void processExpiredDocuments() {
        log.info("Starting retention cleanup job...");
        Instant now = Instant.now();
        UUID tenantId = tenantContext.getCurrentTenantId();
        
        try {
            List<Document> expiredDocuments = documentRepository.findExpiredDocuments(tenantId, now);
            
            log.info("Found {} documents eligible for hard delete", expiredDocuments.size());
            
            for (Document document : expiredDocuments) {
                try {
                    if (legalHoldService.hasActiveLegalHolds(document.getId())) {
                        log.warn("Skipping document with active legal hold: {}", document.getId());
                        continue;
                    }
                    
                    documentService.hardDeleteDocument(document.getId());
                    
                    log.info("Hard deleted expired document: documentId={}, tenant={}", 
                        document.getId(), document.getTenantId());
                } catch (Exception e) {
                    log.error("Failed to hard delete expired document: {}", document.getId(), e);
                }
            }
            
            log.info("Retention cleanup job completed");
        } catch (Exception e) {
            log.error("Retention cleanup job failed", e);
        }
    }
    
    @Transactional(readOnly = true)
    public long getExpiredDocumentsCount() {
        UUID tenantId = tenantContext.getCurrentTenantId();
        return documentRepository.findExpiredDocuments(tenantId, Instant.now()).size();
    }
    
    @Transactional(readOnly = true)
    public long getDocumentsWithActiveLegalHolds() {
        UUID tenantId = tenantContext.getCurrentTenantId();
        List<Document> allDocuments = documentRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
        return allDocuments.stream()
            .filter(d -> legalHoldService.hasActiveLegalHolds(d.getId()))
            .count();
    }
}
