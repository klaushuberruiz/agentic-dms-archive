package com.dms.controller;

import com.dms.mcp.McpToolHandler;
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
    @PreAuthorize("permitAll")
    public ResponseEntity<List<Map<String, Object>>> searchDocuments(
            @RequestBody SearchDocumentsRequest request) {
        log.info("MCP: POST /search_documents");
        List<Map<String, Object>> results = mcpToolHandler.searchDocuments(
            request.getQuery(),
            request.getDocumentType(),
            request.getPageSize()
        );
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/get_document")
    @PreAuthorize("permitAll")
    public ResponseEntity<Map<String, Object>> getDocument(
            @RequestBody GetDocumentRequest request) {
        log.info("MCP: POST /get_document");
        Map<String, Object> result = mcpToolHandler.getDocument(request.getDocumentId());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/search_requirements")
    @PreAuthorize("permitAll")
    public ResponseEntity<List<Map<String, Object>>> searchRequirements(
            @RequestBody SearchRequirementsRequest request) {
        log.info("MCP: POST /search_requirements");
        List<Map<String, Object>> results = mcpToolHandler.searchRequirements(
            request.getQuery(),
            request.getLimit()
        );
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/get_requirement_by_id")
    @PreAuthorize("permitAll")
    public ResponseEntity<Map<String, Object>> getRequirementById(
            @RequestBody GetRequirementRequest request) {
        log.info("MCP: POST /get_requirement_by_id");
        Map<String, Object> result = mcpToolHandler.getRequirementById(request.getChunkId());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/get_related_requirements")
    @PreAuthorize("permitAll")
    public ResponseEntity<List<Map<String, Object>>> getRelatedRequirements(
            @RequestBody RelatedRequirementsRequest request) {
        log.info("MCP: POST /get_related_requirements");
        List<Map<String, Object>> results = mcpToolHandler.getRelatedRequirements(
            request.getChunkId(),
            request.getLimit()
        );
        return ResponseEntity.ok(results);
    }
    
    // Request DTOs
    
    public static class SearchDocumentsRequest {
        private String query;
        private String documentType;
        private Integer pageSize;
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }
    
    public static class GetDocumentRequest {
        private String documentId;
        
        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
    }
    
    public static class SearchRequirementsRequest {
        private String query;
        private Integer limit;
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }
    
    public static class GetRequirementRequest {
        private String chunkId;
        
        public String getChunkId() { return chunkId; }
        public void setChunkId(String chunkId) { this.chunkId = chunkId; }
    }
    
    public static class RelatedRequirementsRequest {
        private String chunkId;
        private Integer limit;
        
        public String getChunkId() { return chunkId; }
        public void setChunkId(String chunkId) { this.chunkId = chunkId; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }
}
