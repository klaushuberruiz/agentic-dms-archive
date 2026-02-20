package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.DocumentType;
import com.dms.dto.request.DocumentUploadRequest;
import com.dms.dto.response.DocumentResponse;
import com.dms.exception.DocumentNotFoundException;
import com.dms.repository.DocumentRepository;
import com.dms.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final TenantContext tenantContext;
    private final BlobStorageService blobStorageService;
    private final MetadataValidationService metadataValidationService;
    private final AuditService auditService;
    private final AuthorizationService authorizationService;
    private final LegalHoldService legalHoldService;
    
    @Transactional
    public DocumentResponse uploadDocument(DocumentUploadRequest request, MultipartFile file) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        
        // Validate document type
        DocumentType documentType = documentTypeRepository
            .findByIdAndTenantId(request.getDocumentTypeId(), tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document type not found"));

        if (Boolean.FALSE.equals(documentType.getActive())) {
            throw new com.dms.exception.ValidationException("Document type is deactivated");
        }

        if (!authorizationService.canUploadToType(documentType.getAllowedGroups())) {
            throw new com.dms.exception.UnauthorizedAccessException("User cannot upload to this document type");
        }
        
        // Validate metadata against schema
        metadataValidationService.validate(request.getMetadata(), documentType.getMetadataSchema());
        
        // Generate blob path
        Instant now = Instant.now();
        UUID documentId = UUID.randomUUID();
        String blobPath = generateBlobPath(tenantId, documentType.getName(), now, documentId, 1);
        
        // Upload to blob storage
        blobStorageService.uploadBlob(blobPath, file);
        
        // Calculate retention expiration
        Instant retentionExpiresAt = now.plus(documentType.getRetentionDays(), ChronoUnit.DAYS);
        
        // Create document entity
        Document document = Document.builder()
            .id(documentId)
            .tenantId(tenantId)
            .documentType(documentType)
            .currentVersion(1)
            .metadata(request.getMetadata())
            .blobPath(blobPath)
            .fileSizeBytes(file.getSize())
            .contentType(file.getContentType())
            .createdAt(now)
            .createdBy(userId)
            .retentionExpiresAt(retentionExpiresAt)
            .build();
        
        document = documentRepository.save(document);
        
        // Audit log
        auditService.logDocumentUpload(document);
        
        return mapToResponse(document);
    }
    
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        
        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        authorizationService.assertCanAccessDocument(document);
        
        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(Pageable pageable) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        return documentRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable)
            .map(this::mapToResponse);
    }

    @Transactional
    public void softDeleteDocument(UUID documentId, String reason) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();

        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        authorizationService.assertCanAccessDocument(document);

        // Check legal holds
        if (legalHoldService.hasActiveLegalHolds(document.getId())) {
            throw new com.dms.exception.LegalHoldActiveException("Document has active legal hold");
        }

        document.setDeletedAt(java.time.Instant.now());
        document.setDeletedBy(userId);
        documentRepository.save(document);

        auditService.logSoftDelete(document.getId(), reason);
    }

    @Transactional
    public void restoreDocument(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();

        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        authorizationService.assertCanAccessDocument(document);

        if (document.getDeletedAt() == null) {
            return;
        }

        java.time.Instant deletedAt = document.getDeletedAt();
        if (deletedAt == null) {
            return;
        }

        // 30 day recovery window
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant allowed = deletedAt.plus(30, java.time.temporal.ChronoUnit.DAYS);
        if (now.isAfter(allowed)) {
            throw new com.dms.exception.ValidationException("Recovery window exceeded");
        }

        document.setDeletedAt(null);
        document.setDeletedBy(null);
        documentRepository.save(document);

        auditService.logRestore(document.getId(), userId);
    }

    @Transactional
    public DocumentResponse updateMetadata(UUID documentId, java.util.Map<String, Object> metadata) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();

        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        // Validate against document type schema
        metadataValidationService.validate(metadata, document.getDocumentType().getMetadataSchema());

        java.util.Map<String, Object> before = document.getMetadata();
        document.setMetadata(metadata);
        document.setModifiedAt(java.time.Instant.now());
        document.setModifiedBy(userId);
        document = documentRepository.save(document);

        auditService.logMetadataUpdate(document.getId(), before, metadata);

        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public java.io.InputStream downloadDocument(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();

        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        authorizationService.assertCanAccessDocument(document);

        auditService.logDocumentDownload(document.getId());

        return blobStorageService.downloadBlob(document.getBlobPath());
    }

    @Transactional(readOnly = true)
    public java.io.InputStream previewDocument(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();

        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        authorizationService.assertCanAccessDocument(document);

        auditService.logDocumentPreview(document.getId());

        return blobStorageService.downloadBlob(document.getBlobPath());
    }
    
    @Transactional
    public void hardDeleteDocument(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        
        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        if (!authorizationService.canDeleteDocument(document)) {
            throw new com.dms.exception.UnauthorizedAccessException("User cannot hard-delete document");
        }
        
        // Delete blob from storage
        blobStorageService.deleteBlob(document.getBlobPath());
        
        // Delete document and related entities
        documentRepository.delete(document);
        auditService.logSoftDelete(documentId, "HARD_DELETE");
        
        log.info("Hard deleted document: documentId={}", documentId);
    }
    
    private String generateBlobPath(UUID tenantId, String documentType, Instant timestamp, UUID documentId, int version) {
        int year = timestamp.atZone(java.time.ZoneOffset.UTC).getYear();
        int month = timestamp.atZone(java.time.ZoneOffset.UTC).getMonthValue();
        return String.format("%s/%s/%d/%02d/%s_v%d.pdf", 
            tenantId, documentType, year, month, documentId, version);
    }
    
    private DocumentResponse mapToResponse(Document document) {
        return DocumentResponse.builder()
            .id(document.getId())
            .documentTypeName(document.getDocumentType().getName())
            .currentVersion(document.getCurrentVersion())
            .metadata(document.getMetadata())
            .fileSizeBytes(document.getFileSizeBytes())
            .createdAt(document.getCreatedAt())
            .createdBy(document.getCreatedBy())
            .modifiedAt(document.getModifiedAt())
            .modifiedBy(document.getModifiedBy())
            .hasActiveLegalHold(legalHoldService.hasActiveLegalHolds(document.getId()))
            .retentionExpiresAt(document.getRetentionExpiresAt())
            .build();
    }
}
