package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.DocumentType;
import com.dms.dto.response.RetentionStatusResponse;
import com.dms.exception.DocumentNotFoundException;
import com.dms.exception.ValidationException;
import com.dms.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${dms.retention.minimum-days:0}")
    private int globalMinimumRetentionDays;

    @Value("${dms.retention.warning-window-days:30}")
    private int warningWindowDays;

    @Value("${dms.retention.tenant-overrides:}")
    private String tenantOverrides;
    
    @Transactional
    public void markForDeletion(UUID documentId, Integer retentionDays) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        
        // Validate document
        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        
        if (document.getDeletedAt() != null) {
            throw new ValidationException("Cannot set retention on deleted document");
        }
        
        // Validate retention days
        if (retentionDays == null || retentionDays < 0) {
            throw new ValidationException("Retention days must be non-negative");
        }
        if (retentionDays > 36500) { // ~100 years
            throw new ValidationException("Retention days cannot exceed 36500 (100 years)");
        }

        int minDays = resolveMinRetentionDays(tenantId, document.getDocumentType());
        if (retentionDays < minDays) {
            throw new ValidationException("Retention days must be at least " + minDays);
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
        if (docType.getRetentionDays() != null && docType.getRetentionDays() > 0) {
            markForDeletion(documentId, docType.getRetentionDays());
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
            .defaultRetentionDays(document.getDocumentType().getRetentionDays())
            .retentionExpiresAt(retentionExpiresAt)
            .daysUntilRetention(daysUntilRetention)
            .hasActiveLegalHolds(hasActiveLegalHolds)
            .isEligibleForHardDelete(isEligibleForHardDelete)
            .isSoftDeleted(document.getDeletedAt() != null)
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

            List<Document> warningCandidates = documentRepository.findByTenantIdAndDeletedAtIsNull(tenantId).stream()
                .filter(d -> d.getRetentionExpiresAt() != null)
                .filter(d -> {
                    long daysUntil = ChronoUnit.DAYS.between(now, d.getRetentionExpiresAt());
                    return daysUntil >= 0 && daysUntil <= warningWindowDays;
                })
                .toList();
            if (!warningCandidates.isEmpty()) {
                log.info("Retention warning window hit for {} documents ({} days)", warningCandidates.size(), warningWindowDays);
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

    private int resolveMinRetentionDays(UUID tenantId, DocumentType docType) {
        int minFromType = docType.getMinRetentionDays() == null ? 0 : docType.getMinRetentionDays();
        int minFromGlobal = Math.max(0, globalMinimumRetentionDays);
        int minFromTenantOverride = parseTenantOverride(tenantId).orElse(0);
        return Math.max(minFromType, Math.max(minFromGlobal, minFromTenantOverride));
    }

    private java.util.Optional<Integer> parseTenantOverride(UUID tenantId) {
        if (tenantOverrides == null || tenantOverrides.isBlank()) {
            return java.util.Optional.empty();
        }
        String[] entries = tenantOverrides.split(",");
        for (String entry : entries) {
            String[] parts = entry.trim().split(":");
            if (parts.length != 2) {
                continue;
            }
            if (!tenantId.toString().equalsIgnoreCase(parts[0].trim())) {
                continue;
            }
            try {
                return java.util.Optional.of(Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
                return java.util.Optional.empty();
            }
        }
        return java.util.Optional.empty();
    }
}
