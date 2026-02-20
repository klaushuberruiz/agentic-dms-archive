package com.dms.controller;

import com.dms.dto.request.GetDocumentRequest;
import com.dms.dto.request.GetRequirementRequest;
import com.dms.dto.request.RelatedRequirementsRequest;
import com.dms.dto.request.SearchDocumentsRequest;
import com.dms.dto.request.SearchRequirementsRequest;
import com.dms.mcp.McpToolHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp/tools")
@RequiredArgsConstructor
@Slf4j
public class McpToolController {
    
    private final McpToolHandler mcpToolHandler;
    
    @PostMapping("/search_documents")
    @PreAuthorize("hasRole('DOCUMENT_USER')")
    public ResponseEntity<List<Map<String, Object>>> searchDocuments(
            @Valid @RequestBody SearchDocumentsRequest request) {
        log.info("MCP: POST /search_documents");
        List<Map<String, Object>> results = mcpToolHandler.searchDocuments(
            request.getQuery(),
            request.getDocumentType(),
            request.getPageSize()
        );
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/get_document")
    @PreAuthorize("hasRole('DOCUMENT_USER')")
    public ResponseEntity<Map<String, Object>> getDocument(
            @Valid @RequestBody GetDocumentRequest request) {
        log.info("MCP: POST /get_document");
        Map<String, Object> result = mcpToolHandler.getDocument(request.getDocumentId());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/search_requirements")
    @PreAuthorize("hasRole('DOCUMENT_USER')")
    public ResponseEntity<List<Map<String, Object>>> searchRequirements(
            @Valid @RequestBody SearchRequirementsRequest request) {
        log.info("MCP: POST /search_requirements");
        List<Map<String, Object>> results = mcpToolHandler.searchRequirements(
            request.getQuery(),
            request.getLimit()
        );
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/get_requirement_by_id")
    @PreAuthorize("hasRole('DOCUMENT_USER')")
    public ResponseEntity<Map<String, Object>> getRequirementById(
            @Valid @RequestBody GetRequirementRequest request) {
        log.info("MCP: POST /get_requirement_by_id");
        Map<String, Object> result = mcpToolHandler.getRequirementById(request.getChunkId());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/get_related_requirements")
    @PreAuthorize("hasRole('DOCUMENT_USER')")
    public ResponseEntity<List<Map<String, Object>>> getRelatedRequirements(
            @Valid @RequestBody RelatedRequirementsRequest request) {
        log.info("MCP: POST /get_related_requirements");
        List<Map<String, Object>> results = mcpToolHandler.getRelatedRequirements(
            request.getChunkId(),
            request.getLimit()
        );
        return ResponseEntity.ok(results);
    }
}
