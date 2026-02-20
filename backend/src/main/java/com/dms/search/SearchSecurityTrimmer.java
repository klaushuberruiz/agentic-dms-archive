package com.dms.search;

import com.dms.dto.response.HybridSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class SearchSecurityTrimmer {

    public List<HybridSearchResult> trimByAllowedDocuments(List<HybridSearchResult> input, Set<UUID> allowedDocumentIds) {
        if (allowedDocumentIds == null || allowedDocumentIds.isEmpty()) {
            return List.of();
        }
        return input.stream()
            .filter(item -> allowedDocumentIds.contains(item.getDocumentId()))
            .toList();
    }
}
