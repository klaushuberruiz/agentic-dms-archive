package com.dms.indexing;

import com.dms.domain.Document;
import com.dms.repository.DocumentRepository;
import com.dms.service.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndexRebuildService {

    private final DocumentRepository documentRepository;
    private final IndexingService indexingService;
    private final TenantContext tenantContext;

    @Transactional
    public int rebuildCurrentTenantIndex() {
        UUID tenantId = tenantContext.getCurrentTenantId();
        List<Document> documents = documentRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
        for (Document document : documents) {
            indexingService.enqueueDocumentIndex(document.getId(), "UPSERT");
        }
        return documents.size();
    }
}
