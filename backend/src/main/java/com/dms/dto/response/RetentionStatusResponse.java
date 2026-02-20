package com.dms.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetentionStatusResponse {
    private UUID documentId;
    private String documentType;
    private Integer defaultRetentionDays;
    private Instant retentionExpiresAt;
    private Long daysUntilRetention;
    private boolean hasActiveLegalHolds;
    private boolean isEligibleForHardDelete;
    private boolean isSoftDeleted;
}
