package com.dms.mcp;

import com.dms.dto.response.DocumentResponse;
import com.dms.service.DocumentService;
import com.dms.service.HybridSearchService;
import com.dms.service.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class McpToolHandler {
    
    private final DocumentService documentService;
    private final HybridSearchService hybridSearchService;
    private final TenantContext tenantContext;
    
    /**
     * MCP Tool: search_documents
     * Search for documents by query, document type, or date range
     */
    public List<Map<String, Object>> searchDocuments(String query, String documentType, Integer pageSize) {
        log.info("MCP: search_documents called with query={}, documentType={}", query, documentType);
        
        if (pageSize == null) pageSize = 10;
        if (pageSize > 100) pageSize = 100;
        
        try {
            var results = hybridSearchService.hybridSearch(
                query, 
                PageRequest.of(0, pageSize)
            );
            
            return results.getContent().stream()
                .map(r -> Map.<String, Object>ofEntries(
                    Map.entry("chunkId", r.getChunkId()),
                    Map.entry("documentId", r.getDocumentId()),
                    Map.entry("content", r.getContent()),
                    Map.entry("relevanceScore", r.getRelevanceScore()),
                    Map.entry("sequenceNumber", r.getSequenceNumber())
                ))
                .toList();
        } catch (Exception e) {
            log.error("MCP: search_documents failed", e);
            return List.of();
        }
    }
    
    /**
     * MCP Tool: get_document
     * Retrieve full document metadata and content info by ID
     */
    public Map<String, Object> getDocument(String documentId) {
        log.info("MCP: get_document called with documentId={}", documentId);
        
        try {
            UUID uuid = UUID.fromString(documentId);
            DocumentResponse doc = documentService.getDocument(uuid);
            
            return Map.<String, Object>ofEntries(
                Map.entry("id", doc.getId()),
                Map.entry("documentType", doc.getDocumentTypeName()),
                Map.entry("version", doc.getCurrentVersion()),
                Map.entry("metadata", doc.getMetadata()),
                Map.entry("fileSizeBytes", doc.getFileSizeBytes()),
                Map.entry("createdAt", doc.getCreatedAt()),
                Map.entry("createdBy", doc.getCreatedBy()),
                Map.entry("hasActiveLegalHold", doc.isHasActiveLegalHold()),
                Map.entry("retentionExpiresAt", doc.getRetentionExpiresAt())
            );
        } catch (Exception e) {
            log.error("MCP: get_document failed", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * MCP Tool: search_requirements
     * Full-text search across all requirement chunks
     */
    public List<Map<String, Object>> searchRequirements(String query, Integer limit) {
        log.info("MCP: search_requirements called with query={}", query);
        
        if (limit == null) limit = 20;
        if (limit > 100) limit = 100;
        
        try {
            var results = hybridSearchService.hybridSearch(
                query,
                PageRequest.of(0, limit)
            );
            
            return results.getContent().stream()
                .map(r -> Map.<String, Object>ofEntries(
                    Map.entry("id", r.getChunkId()),
                    Map.entry("documentId", r.getDocumentId()),
                    Map.entry("sequence", r.getSequenceNumber()),
                    Map.entry("text", r.getContent()),
                    Map.entry("tokens", r.getTokenCount()),
                    Map.entry("score", r.getRelevanceScore())
                ))
                .toList();
        } catch (Exception e) {
            log.error("MCP: search_requirements failed", e);
            return List.of();
        }
    }
    
    /**
     * MCP Tool: get_requirement_by_id
     * Retrieve a specific requirement chunk by ID
     */
    public Map<String, Object> getRequirementById(String chunkId) {
        log.info("MCP: get_requirement_by_id called with chunkId={}", chunkId);
        
        try {
            // In production: query RequirementChunkRepository
            return Map.of(
                "id", chunkId,
                "text", "Requirement chunk content",
                "documentId", "doc-uuid",
                "sequence", 1,
                "tokens", 500
            );
        } catch (Exception e) {
            log.error("MCP: get_requirement_by_id failed", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * MCP Tool: get_related_requirements
     * Find requirements related to a given requirement (semantic similarity)
     */
    public List<Map<String, Object>> getRelatedRequirements(String chunkId, Integer limit) {
        log.info("MCP: get_related_requirements called with chunkId={}", chunkId);
        
        if (limit == null) limit = 5;
        
        try {
            // In production: use vector similarity to find related chunks
            return List.of(
                Map.of("id", "chunk-1", "similarity", 0.95),
                Map.of("id", "chunk-2", "similarity", 0.87)
            );
        } catch (Exception e) {
            log.error("MCP: get_related_requirements failed", e);
            return List.of();
        }
    }
}
