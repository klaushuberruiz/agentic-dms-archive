package com.dms.search;

import com.dms.domain.RequirementChunk;
import com.dms.dto.response.HybridSearchResult;
import com.dms.repository.RequirementChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HybridSearchRouter {

    private final RequirementChunkRepository requirementChunkRepository;

    public List<HybridSearchResult> keywordSearch(String query, UUID tenantId, int limit) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return requirementChunkRepository.findByTenantId(tenantId).stream()
            .filter(chunk -> chunk.getChunkText() != null && chunk.getChunkText().toLowerCase(Locale.ROOT).contains(normalized))
            .limit(Math.max(1, limit))
            .map(this::map)
            .toList();
    }

    public List<HybridSearchResult> vectorSearch(String query, UUID tenantId, int limit) {
        String[] terms = (query == null ? "" : query.toLowerCase(Locale.ROOT)).split("\\s+");
        return requirementChunkRepository.findByTenantId(tenantId).stream()
            .map(chunk -> {
                HybridSearchResult result = map(chunk);
                String text = chunk.getChunkText() == null ? "" : chunk.getChunkText().toLowerCase(Locale.ROOT);
                int hits = 0;
                for (String term : terms) {
                    if (!term.isBlank() && text.contains(term)) {
                        hits++;
                    }
                }
                double score = terms.length == 0 ? 0.0 : (double) hits / (double) terms.length;
                result.setRelevanceScore(score);
                return result;
            })
            .filter(item -> item.getRelevanceScore() > 0)
            .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
            .limit(Math.max(1, limit))
            .toList();
    }

    private HybridSearchResult map(RequirementChunk chunk) {
        return HybridSearchResult.builder()
            .chunkId(chunk.getId())
            .documentId(chunk.getDocumentId())
            .sequenceNumber(chunk.getChunkOrder())
            .content(chunk.getChunkText())
            .tokenCount(chunk.getTokenCount())
            .relevanceScore(1.0)
            .createdAt(chunk.getCreatedAt())
            .build();
    }
}
