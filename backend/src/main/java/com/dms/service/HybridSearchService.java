package com.dms.service;

import com.dms.domain.Document;
import com.dms.dto.response.HybridSearchResult;
import com.dms.repository.DocumentRepository;
import com.dms.search.HybridSearchRouter;
import com.dms.search.SearchFallbackHandler;
import com.dms.search.SearchScoreMerger;
import com.dms.search.SearchSecurityTrimmer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridSearchService {

    private final HybridSearchRouter hybridSearchRouter;
    private final SearchSecurityTrimmer searchSecurityTrimmer;
    private final SearchScoreMerger searchScoreMerger;
    private final SearchFallbackHandler searchFallbackHandler;
    private final DocumentRepository documentRepository;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    @Value("${dms.search.hybrid.keyword-weight:0.4}")
    private double keywordWeight;

    @Value("${dms.search.hybrid.vector-weight:0.6}")
    private double vectorWeight;

    @Value("${dms.search.hybrid.max-results:100}")
    private int maxResults;

    @Transactional(readOnly = true)
    @Cacheable(value = "hybridSearch", key = "T(java.util.Objects).hash(#query + ':' + #pageable.pageNumber + ':' + #pageable.pageSize)")
    public Page<HybridSearchResult> hybridSearch(String query, Pageable pageable) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        List<HybridSearchResult> keywordResults = hybridSearchRouter.keywordSearch(query, tenantId, maxResults);
        List<HybridSearchResult> vectorResults = hybridSearchRouter.vectorSearch(query, tenantId, maxResults);

        List<HybridSearchResult> merged = searchScoreMerger.merge(keywordResults, vectorResults, keywordWeight, vectorWeight, maxResults);

        Set<UUID> allowedDocumentIds = merged.stream()
            .map(HybridSearchResult::getDocumentId)
            .filter(id -> id != null)
            .filter(id -> documentRepository.findByIdAndTenantId(id, tenantId)
                .map(this::canAccess)
                .orElse(false))
            .collect(Collectors.toSet());

        List<HybridSearchResult> secured = searchSecurityTrimmer.trimByAllowedDocuments(merged, allowedDocumentIds);
        if (secured.isEmpty()) {
            secured = searchFallbackHandler.fallback(query);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), secured.size());
        List<HybridSearchResult> paged = start >= secured.size() ? List.of() : secured.subList(start, end);

        auditService.logSearch(query, paged.size());
        log.info("Hybrid search completed: query={}, results={}, userId={}", query, paged.size(), userId);

        return new PageImpl<>(paged, pageable, secured.size());
    }

    @CacheEvict(value = "hybridSearch", allEntries = true)
    public void evictCache() {
        // Explicit invalidation endpoint hook for operations/index changes.
    }

    private boolean canAccess(Document document) {
        try {
            return authorizationService.canAccessDocument(document);
        } catch (Exception ex) {
            return false;
        }
    }
}
