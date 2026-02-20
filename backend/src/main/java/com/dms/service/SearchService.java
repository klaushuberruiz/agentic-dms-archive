package com.dms.service;

import com.dms.domain.Document;
import com.dms.exception.ValidationException;
import com.dms.repository.DocumentRepository;
import com.dms.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    
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
        
        List<Document> results;
        
        if (documentTypeId != null) {
            // Type-specific search
            results = documentRepository.findByTenantIdAndDocumentType(tenantId, documentTypeId, validatedPageable)
                .getContent();
        } else if (startDate != null && endDate != null) {
            // Date range search
            results = documentRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate, validatedPageable)
                .getContent();
        } else {
            // General search - all active documents
            results = documentRepository.findByTenantIdAndDeletedAtIsNull(tenantId, validatedPageable)
                .getContent();
        }
        
        // Apply RBAC filtering - try access, filter out unauthorized ones
        results = results.stream()
            .filter(doc -> {
                try {
                    authorizationService.assertCanAccessDocument(doc);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(Collectors.toList());
        
        // Convert to response format
        List<Map<String, Object>> responseList = results.stream()
            .map(this::mapToSearchResult)
            .collect(Collectors.toList());
        
        // Audit log
        auditService.logSearch(query, responseList.size());
        
        log.info("Search executed: query={}, results={}, userId={}", query, responseList.size(), userId);
        
        return new PageImpl<>(responseList, validatedPageable, responseList.size());
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
}

