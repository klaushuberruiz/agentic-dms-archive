package com.dms.indexing;

import com.dms.domain.RequirementChunk;
import com.dms.dto.response.HybridSearchResult;
import com.dms.repository.RequirementChunkRepository;
import com.dms.search.AzureSearchClient;
import com.dms.service.HybridSearchService;
import com.dms.service.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndexDriftService {

    private final RequirementChunkRepository chunkRepository;
    private final AzureSearchClient azureSearchClient;
    private final IndexingService indexingService;
    private final HybridSearchService hybridSearchService;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public Map<String, Integer> analyzeDrift() {
        UUID tenantId = tenantContext.getCurrentTenantId();
        Set<UUID> sourceIds = new HashSet<>(chunkRepository.findByTenantId(tenantId).stream().map(RequirementChunk::getId).toList());
        Set<UUID> indexIds = new HashSet<>(azureSearchClient.all().stream().map(HybridSearchResult::getChunkId).toList());

        Set<UUID> missingInIndex = new HashSet<>(sourceIds);
        missingInIndex.removeAll(indexIds);

        Set<UUID> orphanedInIndex = new HashSet<>(indexIds);
        orphanedInIndex.removeAll(sourceIds);

        return Map.of(
            "sourceChunks", sourceIds.size(),
            "indexedChunks", indexIds.size(),
            "missingInIndex", missingInIndex.size(),
            "orphanedInIndex", orphanedInIndex.size()
        );
    }

    @Transactional
    public Map<String, Integer> reconcileDrift() {
        UUID tenantId = tenantContext.getCurrentTenantId();
        chunkRepository.findByTenantId(tenantId).forEach(indexingService::indexChunk);
        hybridSearchService.evictCache();
        return analyzeDrift();
    }
}
