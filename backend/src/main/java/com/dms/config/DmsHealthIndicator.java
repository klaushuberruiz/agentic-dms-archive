package com.dms.config;

import com.dms.repository.DocumentRepository;
import com.dms.repository.SearchIndexOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("dms")
@RequiredArgsConstructor
public class DmsHealthIndicator implements HealthIndicator {

    private final DocumentRepository documentRepository;
    private final SearchIndexOutboxEventRepository outboxRepository;

    @Override
    public Health health() {
        long totalDocuments = documentRepository.count();
        long pendingOutbox = outboxRepository.findByProcessedAtIsNull().size();
        return Health.up()
            .withDetail("documents", totalDocuments)
            .withDetail("pendingOutboxEvents", pendingOutbox)
            .build();
    }
}
