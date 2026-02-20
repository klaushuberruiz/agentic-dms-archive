package com.dms.service;

import com.dms.domain.DocumentType;
import com.dms.exception.DocumentNotFoundException;
import com.dms.exception.ValidationException;
import com.dms.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentTypeService {

    private final DocumentTypeRepository documentTypeRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    @Transactional
    public DocumentType createDocumentType(DocumentType request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        documentTypeRepository.findByNameAndTenantId(request.getName(), tenantId).ifPresent(existing -> {
            throw new ValidationException("Document type with same name already exists");
        });
        DocumentType entity = DocumentType.builder()
            .tenantId(tenantId)
            .name(request.getName())
            .displayName(request.getDisplayName() == null || request.getDisplayName().isBlank() ? request.getName() : request.getDisplayName())
            .description(request.getDescription())
            .metadataSchema(request.getMetadataSchema() == null ? Map.of() : request.getMetadataSchema())
            .allowedGroups(request.getAllowedGroups() == null ? new UUID[0] : request.getAllowedGroups())
            .retentionDays(request.getRetentionDays() == null ? 2555 : request.getRetentionDays())
            .minRetentionDays(request.getMinRetentionDays() == null ? 0 : request.getMinRetentionDays())
            .active(request.getActive() == null || request.getActive())
            .createdAt(Instant.now())
            .createdBy(userId)
            .modifiedAt(Instant.now())
            .modifiedBy(userId)
            .build();
        DocumentType saved = documentTypeRepository.save(entity);
        auditService.logMetadataUpdate(saved.getId(), Map.of(), Map.of("action", "DOC_TYPE_CREATE", "name", saved.getName()));
        return saved;
    }

    @Transactional
    public DocumentType updateDocumentType(UUID typeId, DocumentType request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        DocumentType existing = documentTypeRepository.findByIdAndTenantId(typeId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document type not found"));
        if (request.getDisplayName() != null) {
            existing.setDisplayName(request.getDisplayName());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getMetadataSchema() != null) {
            existing.setMetadataSchema(request.getMetadataSchema());
        }
        if (request.getAllowedGroups() != null) {
            existing.setAllowedGroups(request.getAllowedGroups());
        }
        if (request.getRetentionDays() != null) {
            existing.setRetentionDays(request.getRetentionDays());
        }
        if (request.getMinRetentionDays() != null) {
            existing.setMinRetentionDays(request.getMinRetentionDays());
        }
        if (request.getActive() != null) {
            existing.setActive(request.getActive());
        }
        existing.setModifiedAt(Instant.now());
        existing.setModifiedBy(userId);
        DocumentType saved = documentTypeRepository.save(existing);
        auditService.logMetadataUpdate(saved.getId(), Map.of(), Map.of("action", "DOC_TYPE_UPDATE"));
        return saved;
    }

    @Transactional
    public void deactivateDocumentType(UUID typeId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        DocumentType existing = documentTypeRepository.findByIdAndTenantId(typeId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document type not found"));
        existing.setActive(false);
        existing.setModifiedAt(Instant.now());
        existing.setModifiedBy(userId);
        documentTypeRepository.save(existing);
        auditService.logMetadataUpdate(existing.getId(), Map.of(), Map.of("action", "DOC_TYPE_DEACTIVATE"));
    }

    @Transactional(readOnly = true)
    public List<DocumentType> getActiveDocumentTypes() {
        UUID tenantId = tenantContext.getCurrentTenantId();
        return documentTypeRepository.findByTenantId(tenantId).stream().filter(DocumentType::getActive).toList();
    }

    @Transactional(readOnly = true)
    public Page<DocumentType> listDocumentTypes(Pageable pageable) {
        return documentTypeRepository.findByTenantId(tenantContext.getCurrentTenantId(), pageable);
    }

    @Transactional(readOnly = true)
    public DocumentType getDocumentType(UUID typeId) {
        return documentTypeRepository.findByIdAndTenantId(typeId, tenantContext.getCurrentTenantId())
            .orElseThrow(() -> new DocumentNotFoundException("Document type not found"));
    }
}

