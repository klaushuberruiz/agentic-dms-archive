package com.dms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID id;
    private String documentTypeName;
    private Integer currentVersion;
    private Map<String, Object> metadata;
    private Long fileSizeBytes;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
    private boolean hasActiveLegalHold;
    private Instant retentionExpiresAt;
}
