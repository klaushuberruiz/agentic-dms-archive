package com.dms.service;

import com.dms.domain.RequirementChunk;
import com.dms.dto.response.HybridSearchResult;
import com.dms.repository.RequirementChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridSearchService {
    
    private final RequirementChunkRepository requirementChunkRepository;
    private final EmbeddingService embeddingService;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    
    private static final double KEYWORD_WEIGHT = 0.4;
    private static final double VECTOR_WEIGHT = 0.6;
    private static final int RESULT_LIMIT = 100;
    
    @Transactional(readOnly = true)
    public Page<HybridSearchResult> hybridSearch(String query, Pageable pageable) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        
        try {
            // Perform keyword search (PostgreSQL full-text search)
            List<RequirementChunk> keywordResults = performKeywordSearch(query, tenantId);
            
            // Generate embedding for query and perform vector search
            List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
            List<RequirementChunk> vectorResults = performVectorSearch(queryEmbedding, tenantId);
            
            // Combine and score results
            Map<UUID, HybridSearchResult> resultMap = new HashMap<>();
            
            // Score keyword results
            for (int i = 0; i < keywordResults.size(); i++) {
                RequirementChunk chunk = keywordResults.get(i);
                double keywordScore = (1.0 - (i / (double) (keywordResults.size() + 1))) * KEYWORD_WEIGHT;
                
                resultMap.put(chunk.getId(), HybridSearchResult.builder()
                    .chunkId(chunk.getId())
                    .documentId(chunk.getDocumentId())
                    .sequenceNumber(chunk.getChunkOrder())
                    .content(chunk.getChunkText())
                    .tokenCount(chunk.getTokenCount())
                    .relevanceScore(keywordScore)
                    .searchType("keyword")
                    .createdAt(chunk.getCreatedAt())
                    .build());
            }
            
            // Score and merge vector results
            for (int i = 0; i < vectorResults.size(); i++) {
                RequirementChunk chunk = vectorResults.get(i);
                double vectorScore = (1.0 - (i / (double) (vectorResults.size() + 1))) * VECTOR_WEIGHT;
                
                if (resultMap.containsKey(chunk.getId())) {
                    // Combine scores for chunks found in both searches
                    HybridSearchResult existing = resultMap.get(chunk.getId());
                    existing.setRelevanceScore(existing.getRelevanceScore() + vectorScore);
                    existing.setSearchType("hybrid");
                } else {
                    resultMap.put(chunk.getId(), HybridSearchResult.builder()
                        .chunkId(chunk.getId())
                        .documentId(chunk.getDocumentId())
                        .sequenceNumber(chunk.getChunkOrder())
                        .content(chunk.getChunkText())
                        .tokenCount(chunk.getTokenCount())
                        .relevanceScore(vectorScore)
                        .searchType("vector")
                        .createdAt(chunk.getCreatedAt())
                        .build());
                }
            }
            
            // Sort by relevance score descending and limit
            List<HybridSearchResult> results = resultMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(RESULT_LIMIT)
                .collect(Collectors.toList());
            
            // Apply pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), results.size());
            List<HybridSearchResult> paged = results.subList(start, end > start ? end : start);
            
            // Audit log
            auditService.logSearch(query, paged.size());
            
            log.info("Hybrid search completed: query={}, results={}, userId={}", query, paged.size(), userId);
            
            return new PageImpl<>(paged, pageable, results.size());
        } catch (Exception e) {
            log.error("Hybrid search failed", e);
            throw new RuntimeException("Search failed", e);
        }
    }
    
    private List<RequirementChunk> performKeywordSearch(String query, UUID tenantId) {
        // In production: implement PostgreSQL full-text search using @Query with tsvector
        // For now, return all chunks and filter by contains (simplistic)
        List<RequirementChunk> chunks = requirementChunkRepository.findByTenantId(tenantId);
        return chunks.stream()
            .filter(c -> c.getChunkText().toLowerCase().contains(query.toLowerCase()))
            .limit(20)
            .toList();
    }
    
    private List<RequirementChunk> performVectorSearch(List<Double> queryEmbedding, UUID tenantId) {
        // In production: use pgvector extension or Azure AI Search for vector similarity
        // For now, return all chunks (would be filtered by cosine similarity in production)
        List<RequirementChunk> chunks = requirementChunkRepository.findByTenantId(tenantId);
        return chunks.stream()
            .limit(20)
            .toList();
    }
}
