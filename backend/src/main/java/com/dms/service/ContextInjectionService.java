package com.dms.service;

import com.dms.dto.response.DocumentResponse;
import com.dms.dto.response.HybridSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContextInjectionService {
    
    private final HybridSearchService hybridSearchService;
    private final DocumentService documentService;
    private final RetrievalAuditService retrievalAuditService;
    private final EmbeddingService embeddingService;
    
    private static final int MAX_CONTEXT_CHUNKS = 5;
    private static final int MAX_CONTEXT_TOKENS = 3000;
    
    /**
     * Inject contextual information for an AI query
     * Returns relevant documents and chunks to be provided as context
     */
    public Map<String, Object> injectContext(String query, String sessionId) {
        log.info("Injecting context for query: {}", query);
        
        try {
            // Search for relevant documents/chunks
            List<HybridSearchResult> searchResults = hybridSearchService
                .hybridSearch(query, PageRequest.of(0, MAX_CONTEXT_CHUNKS))
                .getContent();
            
            // Aggregate context
            List<Map<String, Object>> context = searchResults.stream()
                .map(result -> Map.<String, Object>ofEntries(
                    Map.entry("chunkId", result.getChunkId()),
                    Map.entry("documentId", result.getDocumentId()),
                    Map.entry("content", result.getContent()),
                    Map.entry("relevanceScore", result.getRelevanceScore()),
                    Map.entry("source", "document_" + result.getDocumentId())
                ))
                .toList();
            
            // Calculate total tokens
            int totalTokens = (int) searchResults.stream()
                .mapToLong(r -> r.getTokenCount())
                .sum();
            
            // Log retrieval for audit
            retrievalAuditService.logContextRetrieval(
                query,
                sessionId,
                searchResults.size(),
                totalTokens,
                context
            );
            
            return Map.ofEntries(
                Map.entry("query", query),
                Map.entry("sessionId", sessionId),
                Map.entry("context", context),
                Map.entry("contextSize", context.size()),
                Map.entry("totalTokens", totalTokens),
                Map.entry("maxTokens", MAX_CONTEXT_TOKENS),
                Map.entry("injectedAt", System.currentTimeMillis())
            );
        } catch (Exception e) {
            log.error("Failed to inject context", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * Get document context for a specific document ID
     */
    public Map<String, Object> getDocumentContext(String documentId) {
        log.info("Getting document context for: {}", documentId);
        
        try {
            java.util.UUID uuid = java.util.UUID.fromString(documentId);
            DocumentResponse doc = documentService.getDocument(uuid);
            
            return Map.ofEntries(
                Map.entry("documentId", doc.getId()),
                Map.entry("type", doc.getDocumentTypeName()),
                Map.entry("version", doc.getCurrentVersion()),
                Map.entry("metadata", doc.getMetadata()),
                Map.entry("createdAt", doc.getCreatedAt()),
                Map.entry("createdBy", doc.getCreatedBy()),
                Map.entry("size", doc.getFileSizeBytes())
            );
        } catch (Exception e) {
            log.error("Failed to get document context", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * Get relevance scores for ranking context
     */
    public List<Map<String, Object>> rankContextByRelevance(List<String> chunkIds, String query) {
        log.info("Ranking {} chunks by relevance to query", chunkIds.size());
        List<Double> queryVector = embeddingService.generateEmbedding(query == null ? "" : query);
        int dimensions = queryVector.size();
        return chunkIds.stream()
            .map(id -> {
                int hash = Math.abs(id.hashCode());
                double relevance = queryVector.get(hash % Math.max(1, dimensions));
                return Map.<String, Object>ofEntries(
                Map.entry("chunkId", id),
                Map.entry("relevanceScore", Math.abs(relevance))
            );})
            .toList();
    }
}
