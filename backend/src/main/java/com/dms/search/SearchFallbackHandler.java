package com.dms.search;

import com.dms.dto.response.HybridSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchFallbackHandler {

    public List<HybridSearchResult> fallback(String query) {
        return List.of(
            HybridSearchResult.builder()
                .searchType("fallback")
                .content("No indexed results found for query: " + query)
                .relevanceScore(0.0)
                .tokenCount(0)
                .sequenceNumber(0)
                .build()
        );
    }
}
