package com.dms.search;

import com.dms.dto.response.HybridSearchResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SearchScoreMerger {

    public List<HybridSearchResult> merge(
        List<HybridSearchResult> keywordResults,
        List<HybridSearchResult> vectorResults,
        double keywordWeight,
        double vectorWeight,
        int limit
    ) {
        Map<UUID, HybridSearchResult> merged = new HashMap<>();

        for (HybridSearchResult item : keywordResults) {
            HybridSearchResult copy = copy(item);
            copy.setRelevanceScore((copy.getRelevanceScore() == null ? 0.0 : copy.getRelevanceScore()) * keywordWeight);
            copy.setSearchType("keyword");
            merged.put(copy.getChunkId(), copy);
        }

        for (HybridSearchResult item : vectorResults) {
            HybridSearchResult existing = merged.get(item.getChunkId());
            double vectorScore = (item.getRelevanceScore() == null ? 0.0 : item.getRelevanceScore()) * vectorWeight;
            if (existing == null) {
                HybridSearchResult copy = copy(item);
                copy.setRelevanceScore(vectorScore);
                copy.setSearchType("vector");
                merged.put(copy.getChunkId(), copy);
            } else {
                existing.setRelevanceScore((existing.getRelevanceScore() == null ? 0.0 : existing.getRelevanceScore()) + vectorScore);
                existing.setSearchType("hybrid");
            }
        }

        return merged.values().stream()
            .sorted(Comparator.comparing(HybridSearchResult::getRelevanceScore, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(Math.max(1, limit))
            .toList();
    }

    private HybridSearchResult copy(HybridSearchResult source) {
        return HybridSearchResult.builder()
            .chunkId(source.getChunkId())
            .documentId(source.getDocumentId())
            .sequenceNumber(source.getSequenceNumber())
            .content(source.getContent())
            .tokenCount(source.getTokenCount())
            .relevanceScore(source.getRelevanceScore())
            .searchType(source.getSearchType())
            .createdAt(source.getCreatedAt())
            .build();
    }
}
