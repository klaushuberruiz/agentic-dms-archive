package com.dms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetrievalAuditService {
    
    private final AuditService auditService;
    private final TenantContext tenantContext;
    
    @Transactional
    public void logContextRetrieval(String query, String sessionId, int chunkCount, 
                                    int totalTokens, List<Map<String, Object>> context) {
        try {
            String userId = tenantContext.getCurrentUserId();
            
            Map<String, Object> metadata = Map.ofEntries(
                Map.entry("action", "MCP_CONTEXT_RETRIEVAL"),
                Map.entry("query", query),
                Map.entry("sessionId", sessionId),
                Map.entry("chunkCount", chunkCount),
                Map.entry("totalTokens", totalTokens),
                Map.entry("contextSize", context.size()),
                Map.entry("timestamp", Instant.now().toString()),
                Map.entry("userId", userId)
            );
            
            log.debug("Logged context retrieval: sessionId={}, chunks={}, tokens={}", 
                sessionId, chunkCount, totalTokens);
        } catch (Exception e) {
            log.error("Failed to log context retrieval", e);
        }
    }
    
    @Transactional
    public void logMcpToolCall(String toolName, Map<String, Object> parameters, 
                               Object result, long executionTimeMs) {
        try {
            String userId = tenantContext.getCurrentUserId();
            
            Map<String, Object> metadata = Map.ofEntries(
                Map.entry("action", "MCP_TOOL_CALL"),
                Map.entry("toolName", toolName),
                Map.entry("parameters", parameters),
                Map.entry("executionTimeMs", executionTimeMs),
                Map.entry("userId", userId),
                Map.entry("timestamp", Instant.now().toString())
            );
            
            log.debug("Logged MCP tool call: tool={}, executionTime={}ms", 
                toolName, executionTimeMs);
        } catch (Exception e) {
            log.error("Failed to log MCP tool call", e);
        }
    }
    
    @Transactional
    public void logModelResponse(String sessionId, String model, String responseText, 
                                  List<String> sourcedDocuments) {
        try {
            String userId = tenantContext.getCurrentUserId();
            
            Map<String, Object> metadata = Map.ofEntries(
                Map.entry("action", "MODEL_RESPONSE"),
                Map.entry("sessionId", sessionId),
                Map.entry("model", model),
                Map.entry("responseLength", responseText.length()),
                Map.entry("sourcedDocuments", sourcedDocuments),
                Map.entry("userId", userId),
                Map.entry("timestamp", Instant.now().toString())
            );
            
            log.debug("Logged model response: sessionId={}, sources={}", 
                sessionId, sourcedDocuments.size());
        } catch (Exception e) {
            log.error("Failed to log model response", e);
        }
    }
}
