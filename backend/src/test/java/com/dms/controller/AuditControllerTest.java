package com.dms.controller;

import com.dms.dto.response.AuditLogResponse;
import com.dms.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditController.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    void returnsPagedAuditLogs() throws Exception {
        AuditLogResponse entry = AuditLogResponse.builder()
            .id(UUID.randomUUID())
            .tenantId(UUID.randomUUID())
            .action("UPLOAD")
            .entityType("DOCUMENT")
            .entityId(UUID.randomUUID())
            .userId("alice")
            .timestamp(Instant.now())
            .build();

        when(auditService.getTenantLogs(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entry)));

        mockMvc.perform(get("/api/v1/audit/logs").with(user("compliance").roles("COMPLIANCE_OFFICER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].action").value("UPLOAD"));
    }

    @Test
    void returnsStatistics() throws Exception {
        when(auditService.getStatistics(any(), any())).thenReturn(Map.of("total", 10L));

        mockMvc.perform(get("/api/v1/audit/statistics").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    void exportsCsv() throws Exception {
        when(auditService.exportCsv(any(), any())).thenReturn("id,timestamp\n");

        mockMvc.perform(post("/api/v1/audit/export").param("format", "csv").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string("id,timestamp\n"));
    }
}
