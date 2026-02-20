package com.dms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HybridSearchResult {
    private UUID chunkId;
    private UUID documentId;
    private Integer sequenceNumber;
    private String content;
    private Integer tokenCount;
    private Double relevanceScore;
    private String searchType;  // "keyword", "vector", or "hybrid"
    private Instant createdAt;
}
