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
    
    @Transactional
    public DocumentResponse uploadDocument(DocumentUploadRequest request, MultipartFile file) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        
        // Validate document type
        DocumentType documentType = documentTypeRepository
            .findByIdAndTenantId(request.getDocumentTypeId(), tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document type not found"));
        
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
        
        return mapToResponse(document);
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
            .hasActiveLegalHold(document.hasActiveLegalHold())
            .retentionExpiresAt(document.getRetentionExpiresAt())
            .build();
    }
}
