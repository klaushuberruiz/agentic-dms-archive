package com.dms.service;

import com.dms.domain.AuditLog;
import com.dms.domain.Document;
import com.dms.dto.response.AuditLogResponse;
import com.dms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final TenantContext tenantContext;
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDocumentUpload(Document document) {
        Map<String, Object> details = new HashMap<>();
        details.put("documentType", document.getDocumentType().getName());
        details.put("version", document.getCurrentVersion());
        details.put("fileSize", document.getFileSizeBytes());
        
        createAuditLog("UPLOAD", "DOCUMENT", document.getId(), details);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDocumentDownload(UUID documentId) {
        createAuditLog("DOWNLOAD", "DOCUMENT", documentId, Map.of());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDocumentPreview(UUID documentId) {
        createAuditLog("PREVIEW", "DOCUMENT", documentId, Map.of());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logMetadataUpdate(UUID documentId, Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> details = new HashMap<>();
        details.put("before", before);
        details.put("after", after);
        createAuditLog("METADATA_UPDATE", "DOCUMENT", documentId, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSoftDelete(UUID documentId, String reason) {
        createAuditLog("SOFT_DELETE", "DOCUMENT", documentId, Map.of("reason", reason));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRestore(UUID documentId, String restoredBy) {
        createAuditLog("RESTORE", "DOCUMENT", documentId, Map.of("restoredBy", restoredBy));
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSearch(String query, int resultCount) {
        Map<String, Object> details = new HashMap<>();
        details.put("query", query);
        details.put("resultCount", resultCount);
        createAuditLog("SEARCH", "DOCUMENT", null, details);
    }
    
    private void createAuditLog(String action, String entityType, UUID entityId, Map<String, Object> details) {
        AuditLog auditLog = AuditLog.builder()
            .tenantId(tenantContext.getCurrentTenantId())
            .correlationId(UUID.randomUUID())
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .userId(tenantContext.getCurrentUserId())
            .timestamp(Instant.now())
            .details(details)
            .build();
        
        auditLogRepository.save(auditLog);
        log.info("Audit log created: {} on {} {}", action, entityType, entityId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getTenantLogs(Pageable pageable) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        return auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getLogById(UUID logId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        AuditLog logEntry = auditLogRepository.findByIdAndTenantId(logId, tenantId)
            .orElseThrow(() -> new com.dms.exception.DocumentNotFoundException("Audit log not found"));
        return mapToResponse(logEntry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getDocumentLogs(UUID documentId, Pageable pageable) {
        return auditLogRepository.findByEntityIdOrderByTimestampDesc(documentId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getUserLogs(String userId, Pageable pageable) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        return auditLogRepository.findByTenantIdAndUserIdOrderByTimestampDesc(tenantId, userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics(Instant start, Instant end) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        Instant effectiveStart = start == null ? Instant.now().minusSeconds(30L * 24L * 3600L) : start;
        Instant effectiveEnd = end == null ? Instant.now() : end;
        long total = auditLogRepository.countByTenantIdAndTimestampBetween(tenantId, effectiveStart, effectiveEnd);
        long uploads = auditLogRepository.countByTenantIdAndActionAndTimestampBetween(tenantId, "UPLOAD", effectiveStart, effectiveEnd);
        long downloads = auditLogRepository.countByTenantIdAndActionAndTimestampBetween(tenantId, "DOWNLOAD", effectiveStart, effectiveEnd);
        long searches = auditLogRepository.countByTenantIdAndActionAndTimestampBetween(tenantId, "SEARCH", effectiveStart, effectiveEnd);
        return Map.of("total", total, "uploads", uploads, "downloads", downloads, "searches", searches);
    }

    @Transactional(readOnly = true)
    public String exportCsv(Instant start, Instant end) {
        Page<AuditLogResponse> page = getTenantLogs(Pageable.ofSize(10_000).withPage(0));
        String header = "id,timestamp,userId,action,entityType,entityId\n";
        String rows = page.getContent().stream()
            .map(item -> String.join(",",
                item.getId().toString(),
                item.getTimestamp().toString(),
                sanitizeCsv(item.getUserId()),
                sanitizeCsv(item.getAction()),
                sanitizeCsv(item.getEntityType()),
                item.getEntityId() == null ? "" : item.getEntityId().toString()))
            .collect(Collectors.joining("\n"));
        return header + rows;
    }

    private String sanitizeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", " ");
    }

    private AuditLogResponse mapToResponse(AuditLog logEntry) {
        return AuditLogResponse.builder()
            .id(logEntry.getId())
            .tenantId(logEntry.getTenantId())
            .action(logEntry.getAction())
            .entityType(logEntry.getEntityType())
            .entityId(logEntry.getEntityId())
            .userId(logEntry.getUserId())
            .clientIp(logEntry.getClientIp() == null ? null : logEntry.getClientIp().toString())
            .correlationId(logEntry.getCorrelationId())
            .details(logEntry.getDetails())
            .timestamp(logEntry.getTimestamp())
            .build();
    }
}
