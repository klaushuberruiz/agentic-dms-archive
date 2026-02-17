package com.dms.service;

import com.dms.domain.AuditLog;
import com.dms.domain.Document;
import com.dms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
}
