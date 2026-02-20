package com.dms.service;

import com.dms.domain.SearchIndexOutboxEvent;
import com.dms.repository.SearchIndexOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexOutboxService {
    
    private final SearchIndexOutboxEventRepository outboxRepository;
    private final HybridSearchService hybridSearchService;
    
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MINUTES = 5;
    
    @Transactional
    public void publishEvent(String entityType, String entityId, String action) {
        SearchIndexOutboxEvent event = SearchIndexOutboxEvent.builder()
            .entityType(entityType)
            .entityId(java.util.UUID.fromString(entityId))
            .action(action)
            .createdAt(Instant.now())
            .processedAt(null)
            .retryCount(0)
            .deadLettered(false)
            .build();
        
        outboxRepository.save(event);
        log.debug("Published outbox event: type={}, entityId={}, action={}", entityType, entityId, action);
    }
    
    @Scheduled(fixedDelay = 30000, initialDelay = 5000) // Every 30 seconds
    @Transactional
    public void processOutboxEvents() {
        log.debug("Processing outbox events...");
        
        try {
            // Find unprocessed events
            List<SearchIndexOutboxEvent> unprocessed = outboxRepository.findByProcessedAtIsNull();
            
            for (SearchIndexOutboxEvent event : unprocessed) {
                try {
                    processEvent(event);
                    
                    event.setProcessedAt(Instant.now());
                    outboxRepository.save(event);
                    
                    log.debug("Processed outbox event: id={}, entityId={}", event.getId(), event.getEntityId());
                } catch (Exception e) {
                    handleEventProcessingError(event, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process outbox events", e);
        }
    }
    
    private void processEvent(SearchIndexOutboxEvent event) {
        switch (event.getAction().toLowerCase()) {
            case "index":
                indexDocument(event.getEntityId().toString());
                break;
            case "update":
                updateDocumentIndex(event.getEntityId().toString());
                break;
            case "delete":
                deleteDocumentIndex(event.getEntityId().toString());
                break;
            default:
                log.warn("Unknown outbox action: {}", event.getAction());
        }
    }
    
    private void indexDocument(String documentId) {
        // In production: call Azure AI Search indexing API
        log.debug("Indexing document: {}", documentId);
    }
    
    private void updateDocumentIndex(String documentId) {
        // In production: update document in Azure AI Search
        log.debug("Updating document index: {}", documentId);
    }
    
    private void deleteDocumentIndex(String documentId) {
        // In production: delete document from Azure AI Search
        log.debug("Deleting document from index: {}", documentId);
    }
    
    private void handleEventProcessingError(SearchIndexOutboxEvent event, Exception e) {
        event.setRetryCount(event.getRetryCount() + 1);
        
        if (event.getRetryCount() >= MAX_RETRY_COUNT) {
            log.error("Outbox event failed after {} retries: id={}, error={}", 
                MAX_RETRY_COUNT, event.getId(), e.getMessage());
            event.setDeadLettered(true);
        } else {
            log.warn("Retrying outbox event: id={}, retryCount={}", event.getId(), event.getRetryCount());
            event.setNextRetryAt(Instant.now().plusSeconds(RETRY_DELAY_MINUTES * 60));
        }
        
        outboxRepository.save(event);
    }
}
