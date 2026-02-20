package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.DocumentType;
import com.dms.domain.DocumentVersion;
import com.dms.dto.request.DocumentUploadRequest;
import com.dms.dto.response.DocumentResponse;
import com.dms.exception.DocumentNotFoundException;
import com.dms.exception.RetentionNotExpiredException;
import com.dms.repository.DocumentRepository;
import com.dms.repository.DocumentTypeRepository;
import com.dms.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentVersionRepository documentVersionRepository;
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
        String idempotencyKey = request.getIdempotencyKey() == null ? null : request.getIdempotencyKey().trim();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Document existing = documentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).orElse(null);
            if (existing != null) {
                authorizationService.assertCanAccessDocument(existing);
                return mapToResponse(existing);
            }
        }

        DocumentType documentType = documentTypeRepository
            .findByIdAndTenantId(request.getDocumentTypeId(), tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document type not found"));

        if (Boolean.FALSE.equals(documentType.getActive())) {
            throw new com.dms.exception.ValidationException("Document type is deactivated");
        }

        if (!authorizationService.canUploadToType(documentType.getAllowedGroups())) {
            throw new com.dms.exception.UnauthorizedAccessException("User cannot upload to this document type");
        }

        metadataValidationService.validate(request.getMetadata(), documentType.getMetadataSchema());

        Instant now = Instant.now();
        UUID documentId = UUID.randomUUID();
        String blobPath = generateBlobPath(tenantId, documentType.getName(), now, documentId, 1);

        try {
            String contentHash = blobStorageService.uploadValidatedPdf(blobPath, file);
            Instant retentionExpiresAt = now.plus(documentType.getRetentionDays(), ChronoUnit.DAYS);

            Document document = Document.builder()
                .id(documentId)
                .tenantId(tenantId)
                .documentType(documentType)
                .currentVersion(1)
                .metadata(request.getMetadata())
                .blobPath(blobPath)
                .fileSizeBytes(file.getSize())
                .contentType(file.getContentType() == null ? "application/pdf" : file.getContentType())
                .contentHash(contentHash)
                .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey)
                .createdAt(now)
                .createdBy(userId)
                .retentionExpiresAt(retentionExpiresAt)
                .build();

            document = documentRepository.save(document);
            auditService.logDocumentUpload(document);
            return mapToResponse(document);
        } catch (Exception ex) {
            try {
                blobStorageService.deleteBlob(blobPath);
            } catch (Exception cleanupEx) {
                log.warn("Failed to clean up orphaned blob: {}", blobPath, cleanupEx);
            }
            throw ex;
        }
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

        if (legalHoldService.hasActiveLegalHolds(document.getId())) {
            throw new com.dms.exception.LegalHoldActiveException("Document has active legal hold");
        }

        document.setDeletedAt(Instant.now());
        document.setDeletedBy(userId);
        document.setDeleteReason(reason);
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

        Instant deletedAt = document.getDeletedAt();
        Instant now = Instant.now();
        Instant allowed = deletedAt.plus(30, ChronoUnit.DAYS);
        if (now.isAfter(allowed)) {
            throw new com.dms.exception.ValidationException("Recovery window exceeded");
        }

        document.setDeletedAt(null);
        document.setDeletedBy(null);
        document.setDeleteReason(null);
        documentRepository.save(document);

        auditService.logRestore(document.getId(), userId);
    }

    @Transactional
    public DocumentResponse updateMetadata(UUID documentId, Map<String, Object> metadata) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();

        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        authorizationService.assertCanAccessDocument(document);
        metadataValidationService.validate(metadata, document.getDocumentType().getMetadataSchema());

        Map<String, Object> before = document.getMetadata();
        document.setMetadata(metadata);
        document.setModifiedAt(Instant.now());
        document.setModifiedBy(userId);
        document = documentRepository.save(document);

        auditService.logMetadataUpdate(document.getId(), before, metadata);

        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public InputStream downloadDocument(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();

        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        authorizationService.assertCanAccessDocument(document);
        auditService.logDocumentDownload(document.getId());

        return blobStorageService.downloadBlob(document.getBlobPath());
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocumentRange(UUID documentId, long start, long end) {
        UUID tenantId = tenantContext.getCurrentTenantId();

        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        authorizationService.assertCanAccessDocument(document);
        auditService.logDocumentDownload(document.getId());

        return blobStorageService.downloadBlobRange(document.getBlobPath(), start, end);
    }

    @Transactional(readOnly = true)
    public InputStream previewDocument(UUID documentId) {
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

        Instant now = Instant.now();
        if (document.getRetentionExpiresAt() != null && now.isBefore(document.getRetentionExpiresAt())) {
            throw new RetentionNotExpiredException("Document retention period has not expired");
        }
        if (legalHoldService.hasActiveLegalHolds(documentId)) {
            throw new com.dms.exception.LegalHoldActiveException("Document has active legal hold");
        }

        List<DocumentVersion> versions = documentVersionRepository.findAllByDocumentOrderByVersionNumberDesc(document);
        for (DocumentVersion version : versions) {
            blobStorageService.deleteBlob(version.getBlobPath());
        }
        blobStorageService.deleteBlob(document.getBlobPath());

        documentRepository.delete(document);
        auditService.logHardDelete(documentId, document.getDeleteReason(), versions.size());

        log.info("Hard deleted document: documentId={}", documentId);
    }

    @Transactional(readOnly = true)
    public String generateDownloadUrl(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        authorizationService.assertCanAccessDocument(document);
        return blobStorageService.generateReadSasUrl(document.getBlobPath(), Duration.ofMinutes(60));
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
