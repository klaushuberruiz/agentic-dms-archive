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
public class ChunkMetrics {
    private UUID documentId;
    private int chunkCount;
    private int totalTokens;
    private int averageChunkSize;
    private Instant chunkedAt;
}
