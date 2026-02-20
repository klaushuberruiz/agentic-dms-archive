package com.dms.controller;

import com.dms.dto.request.BulkDownloadRequest;
import com.dms.dto.request.SearchRequest;
import com.dms.dto.response.SearchResultResponse;
import com.dms.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    @PreAuthorize("hasRole('DOCUMENT_USER')")
    public ResponseEntity<SearchResultResponse> search(@Valid @RequestBody SearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }

    @PostMapping("/bulk-download")
    @PreAuthorize("hasRole('DOCUMENT_USER')")
    public ResponseEntity<byte[]> bulkDownload(@Valid @RequestBody BulkDownloadRequest request) {
        byte[] archive = searchService.bulkDownload(request.getDocumentIds());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documents.zip\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(archive);
    }
}
