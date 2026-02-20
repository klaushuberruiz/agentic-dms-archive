package com.dms.controller;

import com.dms.dto.response.AuditLogResponse;
import com.dms.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Page<AuditLogResponse>> getLogs(Pageable pageable) {
        return ResponseEntity.ok(auditService.getTenantLogs(pageable));
    }

    @GetMapping("/logs/{logId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<AuditLogResponse> getLogById(@PathVariable UUID logId) {
        return ResponseEntity.ok(auditService.getLogById(logId));
    }

    @GetMapping("/logs/document/{documentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Page<AuditLogResponse>> getDocumentLogs(@PathVariable UUID documentId, Pageable pageable) {
        return ResponseEntity.ok(auditService.getDocumentLogs(documentId, pageable));
    }

    @GetMapping("/logs/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Page<AuditLogResponse>> getUserLogs(@PathVariable String userId, Pageable pageable) {
        return ResponseEntity.ok(auditService.getUserLogs(userId, pageable));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Map<String, Long>> getStatistics(
        @RequestParam(required = false) Instant startTime,
        @RequestParam(required = false) Instant endTime
    ) {
        return ResponseEntity.ok(auditService.getStatistics(startTime, endTime));
    }

    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> export(
        @RequestParam(defaultValue = "csv") String format,
        @RequestParam(required = false) Instant startTime,
        @RequestParam(required = false) Instant endTime
    ) {
        if ("json".equalsIgnoreCase(format)) {
            byte[] data = auditService.getTenantLogs(Pageable.ofSize(10_000).withPage(0)).getContent().toString().getBytes();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
        }
        byte[] data = auditService.exportCsv(startTime, endTime).getBytes();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.csv\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(data);
    }
}
