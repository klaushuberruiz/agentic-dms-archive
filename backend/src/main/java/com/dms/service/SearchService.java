package com.dms.service;

import com.dms.domain.Document;
import com.dms.dto.request.SearchRequest;
import com.dms.dto.response.DocumentResponse;
import com.dms.dto.response.SearchResultResponse;
import com.dms.repository.DocumentRepository;
import com.dms.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final BlobStorageService blobStorageService;
    
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> search(String query, UUID documentTypeId, Instant startDate, 
                                           Instant endDate, Pageable pageable) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        
        // Validate page size
        int pageSize = validatePageSize(pageable.getPageSize());
        Pageable validatedPageable = Pageable.ofSize(pageSize).withPage(pageable.getPageNumber());
        
        Page<Document> page;
        
        if (documentTypeId != null) {
            // Type-specific search
            page = documentRepository.findByTenantIdAndDocumentTypeId(tenantId, documentTypeId, validatedPageable);
        } else if (query != null && !query.isBlank()) {
            page = documentRepository.searchFullText(tenantId, query, validatedPageable);
        } else if (startDate != null && endDate != null) {
            // Date range search
            page = documentRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate, validatedPageable);
        } else {
            // General search - all active documents
            page = documentRepository.findByTenantIdAndDeletedAtIsNull(tenantId, validatedPageable);
        }
        
        // Apply RBAC filtering - try access, filter out unauthorized ones
        List<Document> results = page.getContent().stream()
            .filter(doc -> {
                try {
                    authorizationService.assertCanAccessDocument(doc);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            })
            .toList();
        
        // Convert to response format
        List<Map<String, Object>> responseList = results.stream()
            .map(this::mapToSearchResult)
            .collect(Collectors.toList());
        
        // Audit log
        auditService.logSearch(query, responseList.size());
        
        log.info("Search executed: query={}, results={}, userId={}", query, responseList.size(), userId);
        
        return new PageImpl<>(responseList, validatedPageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SearchResultResponse search(SearchRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        Pageable pageable = Pageable.ofSize(validatePageSize(request.getPageSize())).withPage(Math.max(0, request.getPage()));
        Page<Document> page;
        if (request.getDocumentType() != null && !request.getDocumentType().isBlank()) {
            UUID typeId = resolveDocumentTypeId(tenantId, request.getDocumentType());
            page = documentRepository.findByTenantIdAndDocumentTypeId(tenantId, typeId, pageable);
        } else if (request.getDateFrom() != null && request.getDateTo() != null) {
            Instant from = request.getDateFrom().atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to = request.getDateTo().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusSeconds(1);
            page = documentRepository.findByTenantIdAndDateRange(tenantId, from, to, pageable);
        } else {
            page = documentRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
        }

        List<DocumentResponse> filtered = page.getContent().stream()
            .filter(document -> request.isIncludeDeleted() || document.getDeletedAt() == null)
            .filter(document -> {
                try {
                    return authorizationService.canAccessDocument(document);
                } catch (Exception ignored) {
                    return false;
                }
            })
            .filter(document -> matchesMetadata(document, request.getMetadata()))
            .map(this::mapToDocumentResponse)
            .toList();

        auditService.logSearch(request.getDocumentType(), filtered.size());
        return SearchResultResponse.builder()
            .results(filtered)
            .totalCount(page.getTotalElements())
            .page(pageable.getPageNumber())
            .pageSize(pageable.getPageSize())
            .totalPages(page.getTotalPages())
            .build();
    }

    @Transactional(readOnly = true)
    public byte[] bulkDownload(List<UUID> documentIds) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        List<Document> documents = new ArrayList<>();
        for (UUID documentId : documentIds) {
            Document document = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new com.dms.exception.DocumentNotFoundException("Document not found: " + documentId));
            authorizationService.assertCanAccessDocument(document);
            documents.add(document);
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Document document : documents) {
                try (InputStream fileStream = blobStorageService.downloadBlob(document.getBlobPath())) {
                    ZipEntry entry = new ZipEntry(document.getId() + ".pdf");
                    zos.putNextEntry(entry);
                    fileStream.transferTo(zos);
                    zos.closeEntry();
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create bulk download archive", ex);
        }
    }
    
    private int validatePageSize(int pageSize) {
        if (pageSize < 1) return DEFAULT_PAGE_SIZE;
        if (pageSize > MAX_PAGE_SIZE) return MAX_PAGE_SIZE;
        return pageSize;
    }
    
    private Map<String, Object> mapToSearchResult(Document document) {
        return Map.ofEntries(
            Map.entry("id", document.getId()),
            Map.entry("documentTypeName", document.getDocumentType().getName()),
            Map.entry("currentVersion", document.getCurrentVersion()),
            Map.entry("metadata", document.getMetadata()),
            Map.entry("fileSizeBytes", document.getFileSizeBytes()),
            Map.entry("createdAt", document.getCreatedAt()),
            Map.entry("createdBy", document.getCreatedBy())
        );
    }

    private UUID resolveDocumentTypeId(UUID tenantId, String documentTypeName) {
        return documentTypeRepository.findByNameAndTenantId(documentTypeName, tenantId)
            .map(type -> type.getId())
            .orElseThrow(() -> new com.dms.exception.DocumentNotFoundException("Document type not found"));
    }

    private boolean matchesMetadata(Document document, Map<String, Object> requestedMetadata) {
        if (requestedMetadata == null || requestedMetadata.isEmpty()) {
            return true;
        }
        Map<String, Object> currentMetadata = document.getMetadata();
        for (Map.Entry<String, Object> entry : requestedMetadata.entrySet()) {
            Object currentValue = currentMetadata.get(entry.getKey());
            if (currentValue == null || !currentValue.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private DocumentResponse mapToDocumentResponse(Document document) {
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
            .hasActiveLegalHold(document.getLegalHolds() != null && document.getLegalHolds().stream().anyMatch(hold -> hold.getReleasedAt() == null))
            .retentionExpiresAt(document.getRetentionExpiresAt())
            .build();
    }
}

