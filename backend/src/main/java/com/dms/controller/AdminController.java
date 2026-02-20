package com.dms.controller;

import com.dms.domain.SearchIndexOutboxEvent;
import com.dms.dto.response.AdminStatusResponse;
import com.dms.service.RetentionService;
import com.dms.repository.SearchIndexOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RetentionService retentionService;
    private final SearchIndexOutboxEventRepository outboxRepository;

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatusResponse> status() {
        return ResponseEntity.ok(AdminStatusResponse.builder()
            .expiredDocumentsCount(retentionService.getExpiredDocumentsCount())
            .documentsWithActiveLegalHoldsCount(retentionService.getDocumentsWithActiveLegalHolds())
            .timestamp(Instant.now().toEpochMilli())
            .build());
    }

    @PostMapping("/retention/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerRetention() {
        retentionService.processExpiredDocuments();
        return ResponseEntity.accepted().body(Map.of("status", "triggered"));
    }

    @GetMapping("/retention/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> retentionCount() {
        return ResponseEntity.ok(Map.of(
            "expiredDocuments", retentionService.getExpiredDocumentsCount(),
            "activeLegalHolds", retentionService.getDocumentsWithActiveLegalHolds()));
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", Instant.now().toString()));
    }

    @GetMapping("/outbox/dead-letters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SearchIndexOutboxEvent>> deadLetters() {
        return ResponseEntity.ok(outboxRepository.findByDeadLetteredTrueOrderByCreatedAtDesc());
    }

    @PostMapping("/outbox/dead-letters/{id}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> replayDeadLetter(@PathVariable UUID id) {
        SearchIndexOutboxEvent event = outboxRepository.findById(id)
            .orElseThrow(() -> new com.dms.exception.DocumentNotFoundException("Outbox event not found"));
        event.setDeadLettered(false);
        event.setRetryCount(0);
        event.setNextRetryAt(Instant.now());
        outboxRepository.save(event);
        return ResponseEntity.accepted().build();
    }
}
