package com.dms.indexing;

import com.dms.domain.RequirementChunk;
import com.dms.domain.SearchIndexOutboxEvent;
import com.dms.repository.RequirementChunkRepository;
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
public class SearchIndexOutboxProcessor {

    private final SearchIndexOutboxEventRepository outboxRepository;
    private final RequirementChunkRepository requirementChunkRepository;
    private final IndexingService indexingService;

    @Transactional
    @Scheduled(fixedDelayString = "${dms.search.outbox.poll-interval-ms:10000}")
    public void processPendingEvents() {
        List<SearchIndexOutboxEvent> pending = outboxRepository.findByProcessedAtIsNullAndDeadLetteredFalseOrderByCreatedAtAsc();
        for (SearchIndexOutboxEvent event : pending) {
            try {
                process(event);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception ex) {
                int retries = event.getRetryCount() == null ? 0 : event.getRetryCount();
                retries++;
                event.setRetryCount(retries);
                event.setNextRetryAt(Instant.now().plusSeconds((long) Math.min(300, retries * 10L)));
                if (retries >= event.getMaxRetries()) {
                    event.setDeadLettered(true);
                    log.error("Outbox event dead-lettered: {}", event.getId(), ex);
                } else {
                    log.warn("Outbox event retry {} for {}", retries, event.getId());
                }
                outboxRepository.save(event);
            }
        }
    }

    private void process(SearchIndexOutboxEvent event) {
        if ("DELETE".equalsIgnoreCase(event.getAction())) {
            indexingService.deleteDocumentFromIndex(event.getEntityId());
            return;
        }
        List<RequirementChunk> chunks = requirementChunkRepository.findByDocumentId(event.getEntityId());
        for (RequirementChunk chunk : chunks) {
            indexingService.indexChunk(chunk);
        }
    }
}
