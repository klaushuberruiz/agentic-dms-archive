package com.dms.mcp;

import com.dms.domain.RequirementChunk;
import com.dms.dto.response.DocumentResponse;
import com.dms.dto.response.HybridSearchResult;
import com.dms.repository.RequirementChunkRepository;
import com.dms.service.DocumentService;
import com.dms.service.HybridSearchService;
import com.dms.service.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class McpToolHandler {

    private final DocumentService documentService;
    private final HybridSearchService hybridSearchService;
    private final RequirementChunkRepository requirementChunkRepository;
    private final TenantContext tenantContext;

    public List<Map<String, Object>> searchDocuments(String query, String documentType, Integer pageSize) {
        log.info("MCP: search_documents called with query={}, documentType={}", query, documentType);

        int size = pageSize == null ? 10 : Math.min(pageSize, 100);
        try {
            var results = hybridSearchService.hybridSearch(query, PageRequest.of(0, size));
            return results.getContent().stream().map(this::toSearchDocumentMap).toList();
        } catch (Exception e) {
            log.error("MCP: search_documents failed", e);
            return List.of();
        }
    }

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

    public List<Map<String, Object>> searchRequirements(String query, Integer limit) {
        log.info("MCP: search_requirements called with query={}", query);

        int size = limit == null ? 20 : Math.min(limit, 100);

        try {
            var results = hybridSearchService.hybridSearch(query, PageRequest.of(0, size));
            return results.getContent().stream().map(this::toRequirementMap).toList();
        } catch (Exception e) {
            log.error("MCP: search_requirements failed", e);
            return List.of();
        }
    }

    public Map<String, Object> getRequirementById(String chunkId) {
        log.info("MCP: get_requirement_by_id called with chunkId={}", chunkId);

        try {
            UUID id = UUID.fromString(chunkId);
            UUID tenantId = tenantContext.getCurrentTenantId();
            RequirementChunk chunk = requirementChunkRepository.findById(id)
                .filter(item -> tenantId.equals(item.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Requirement chunk not found"));

            return Map.<String, Object>ofEntries(
                Map.entry("id", chunk.getId()),
                Map.entry("documentId", chunk.getDocumentId()),
                Map.entry("sequence", chunk.getChunkOrder()),
                Map.entry("text", chunk.getChunkText()),
                Map.entry("tokens", chunk.getTokenCount()),
                Map.entry("module", chunk.getModule() == null ? "" : chunk.getModule())
            );
        } catch (Exception e) {
            log.error("MCP: get_requirement_by_id failed", e);
            return Map.of("error", e.getMessage());
        }
    }

    public List<Map<String, Object>> getRelatedRequirements(String chunkId, Integer limit) {
        log.info("MCP: get_related_requirements called with chunkId={}", chunkId);

        int size = limit == null ? 5 : Math.min(limit, 25);

        try {
            UUID id = UUID.fromString(chunkId);
            UUID tenantId = tenantContext.getCurrentTenantId();
            RequirementChunk seed = requirementChunkRepository.findById(id)
                .filter(item -> tenantId.equals(item.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Requirement chunk not found"));

            String seedModule = seed.getModule();
            List<RequirementChunk> related = requirementChunkRepository.findByTenantId(tenantId).stream()
                .filter(chunk -> !chunk.getId().equals(seed.getId()))
                .filter(chunk -> seedModule == null || seedModule.equalsIgnoreCase(chunk.getModule()))
                .limit(size)
                .toList();

            return related.stream().map(chunk -> Map.<String, Object>ofEntries(
                Map.entry("id", chunk.getId().toString()),
                Map.entry("documentId", chunk.getDocumentId().toString()),
                Map.entry("similarity", 0.8),
                Map.entry("module", chunk.getModule() == null ? "" : chunk.getModule())
            )).toList();
        } catch (Exception e) {
            log.error("MCP: get_related_requirements failed", e);
            return List.of();
        }
    }

    public Map<String, Object> validateRequirementReferences(List<String> chunkIds) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        Map<String, Object> result = new HashMap<>();
        int validCount = 0;
        int invalidCount = 0;

        for (String chunkId : chunkIds) {
            try {
                UUID id = UUID.fromString(chunkId);
                boolean valid = requirementChunkRepository.findById(id)
                    .map(chunk -> tenantId.equals(chunk.getTenantId()))
                    .orElse(false);
                if (valid) {
                    validCount++;
                } else {
                    invalidCount++;
                }
            } catch (Exception ex) {
                invalidCount++;
            }
        }

        result.put("total", chunkIds.size());
        result.put("valid", validCount);
        result.put("invalid", invalidCount);
        result.put("isValid", invalidCount == 0);
        return result;
    }

    private Map<String, Object> toSearchDocumentMap(HybridSearchResult r) {
        return Map.<String, Object>ofEntries(
            Map.entry("chunkId", r.getChunkId()),
            Map.entry("documentId", r.getDocumentId()),
            Map.entry("content", r.getContent()),
            Map.entry("relevanceScore", r.getRelevanceScore()),
            Map.entry("sequenceNumber", r.getSequenceNumber())
        );
    }

    private Map<String, Object> toRequirementMap(HybridSearchResult r) {
        return Map.<String, Object>ofEntries(
            Map.entry("id", r.getChunkId()),
            Map.entry("documentId", r.getDocumentId()),
            Map.entry("sequence", r.getSequenceNumber()),
            Map.entry("text", r.getContent()),
            Map.entry("tokens", r.getTokenCount()),
            Map.entry("score", r.getRelevanceScore())
        );
    }
}
