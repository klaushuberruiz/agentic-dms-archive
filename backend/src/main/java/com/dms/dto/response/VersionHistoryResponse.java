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
public class VersionHistoryResponse {

    private UUID documentId;
    private int versionNumber;
    private String blobPath;
    private long fileSizeBytes;
    private String contentHash;
    private String createdBy;
    private Instant createdAt;
}
