package com.dms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;
    private UUID tenantId;
    private String action;
    private String entityType;
    private UUID entityId;
    private String userId;
    private String clientIp;
    private UUID correlationId;
    private Map<String, Object> details;
    private Instant timestamp;
}
