package com.dms.mapper;

import com.dms.domain.AuditLog;
import com.dms.dto.response.AuditLogResponse;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }

        return AuditLogResponse.builder()
            .id(auditLog.getId())
            .tenantId(auditLog.getTenantId())
            .action(auditLog.getAction())
            .entityType(auditLog.getEntityType())
            .entityId(auditLog.getEntityId())
            .userId(auditLog.getUserId())
            .clientIp(auditLog.getClientIp() == null ? null : auditLog.getClientIp().getHostAddress())
            .correlationId(auditLog.getCorrelationId())
            .details(auditLog.getDetails())
            .timestamp(auditLog.getTimestamp())
            .build();
    }
}
