package com.dms.search;

import com.dms.dto.response.HybridSearchResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AzureSearchClient {

    private final Map<UUID, HybridSearchResult> index = new ConcurrentHashMap<>();

    public void upsert(HybridSearchResult result) {
        index.put(result.getChunkId(), result);
    }

    public void deleteByChunkId(UUID chunkId) {
        index.remove(chunkId);
    }

    public List<HybridSearchResult> search(String query, int limit) {
        String normalized = query == null ? "" : query.toLowerCase();
        return index.values().stream()
            .filter(item -> item.getContent() != null && item.getContent().toLowerCase().contains(normalized))
            .sorted(Comparator.comparing(HybridSearchResult::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(Math.max(1, limit))
            .toList();
    }

    public List<HybridSearchResult> all() {
        return new ArrayList<>(index.values());
    }
}
