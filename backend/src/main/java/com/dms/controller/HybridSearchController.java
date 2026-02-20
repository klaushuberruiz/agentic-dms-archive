package com.dms.controller;

import com.dms.dto.request.HybridSearchRequest;
import com.dms.dto.response.HybridSearchResult;
import com.dms.service.HybridSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search-hybrid")
@RequiredArgsConstructor
@Slf4j
public class HybridSearchController {
    
    private final HybridSearchService hybridSearchService;
    
    @GetMapping
    @PreAuthorize("hasRole('DOCUMENT_USER')")
    public ResponseEntity<Page<HybridSearchResult>> hybridSearch(
            @RequestParam String query,
            Pageable pageable) {
        log.info("Hybrid search request: query={}, page={}", query, pageable.getPageNumber());
        Page<HybridSearchResult> results = hybridSearchService.hybridSearch(query, pageable);
        return ResponseEntity.ok(results);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('DOCUMENT_USER')")
    public ResponseEntity<Page<HybridSearchResult>> hybridSearchPost(
            @Valid @RequestBody HybridSearchRequest request,
            Pageable pageable) {
        log.info("Hybrid search POST request: query={}", request.getQuery());
        Page<HybridSearchResult> results = hybridSearchService.hybridSearch(request.getQuery(), pageable);
        return ResponseEntity.ok(results);
    }
}
