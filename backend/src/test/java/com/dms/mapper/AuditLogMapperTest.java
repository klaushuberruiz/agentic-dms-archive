package com.dms.mapper;

import com.dms.domain.AuditLog;
import com.dms.dto.response.AuditLogResponse;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogMapperTest {

    private final AuditLogMapper mapper = new AuditLogMapper();

    @Test
    void shouldMapAuditLogToResponse() throws Exception {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Instant now = Instant.now();

        AuditLog auditLog = AuditLog.builder()
            .id(id)
            .tenantId(tenantId)
            .action("VIEW")
            .entityType("DOCUMENT")
            .entityId(entityId)
            .userId("user-1")
            .clientIp(InetAddress.getByName("127.0.0.1"))
            .correlationId(correlationId)
            .details(Map.of("key", "value"))
            .timestamp(now)
            .build();

        AuditLogResponse response = mapper.toResponse(auditLog);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getClientIp()).isEqualTo("127.0.0.1");
        assertThat(response.getDetails()).containsEntry("key", "value");
    }
}
