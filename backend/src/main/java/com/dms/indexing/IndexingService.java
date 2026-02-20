package com.dms.indexing;

import com.dms.domain.RequirementChunk;
import com.dms.domain.SearchIndexOutboxEvent;
import com.dms.dto.response.HybridSearchResult;
import com.dms.repository.RequirementChunkRepository;
import com.dms.repository.SearchIndexOutboxEventRepository;
import com.dms.search.AzureSearchClient;
import com.dms.service.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private final SearchIndexOutboxEventRepository outboxRepository;
    private final RequirementChunkRepository requirementChunkRepository;
    private final AzureSearchClient azureSearchClient;
    private final TenantContext tenantContext;

    @Transactional
    public void enqueueDocumentIndex(UUID documentId, String action) {
        SearchIndexOutboxEvent event = SearchIndexOutboxEvent.builder()
            .tenantId(tenantContext.getCurrentTenantId())
            .entityType("DOCUMENT")
            .entityId(documentId)
            .action(action)
            .payload("{}")
            .retryCount(0)
            .maxRetries(5)
            .deadLettered(false)
            .createdAt(Instant.now())
            .build();
        outboxRepository.save(event);
    }

    @Transactional
    public void indexChunk(RequirementChunk chunk) {
        HybridSearchResult result = HybridSearchResult.builder()
            .chunkId(chunk.getId())
            .documentId(chunk.getDocumentId())
            .sequenceNumber(chunk.getChunkOrder())
            .content(chunk.getChunkText())
            .tokenCount(chunk.getTokenCount())
            .relevanceScore(1.0)
            .searchType("indexed")
            .createdAt(chunk.getCreatedAt())
            .build();
        azureSearchClient.upsert(result);
    }

    @Transactional
    public void deleteDocumentFromIndex(UUID documentId) {
        requirementChunkRepository.findByDocumentId(documentId)
            .forEach(chunk -> azureSearchClient.deleteByChunkId(chunk.getId()));
    }
}
