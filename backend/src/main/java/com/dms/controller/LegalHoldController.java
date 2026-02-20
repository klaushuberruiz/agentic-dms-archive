package com.dms.controller;

import com.dms.domain.LegalHold;
import com.dms.dto.request.LegalHoldRequest;
import com.dms.service.LegalHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/legal-holds")
@RequiredArgsConstructor
public class LegalHoldController {

    private final LegalHoldService legalHoldService;

    @PostMapping
    @PreAuthorize("hasRole('LEGAL_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<LegalHold> place(@Valid @RequestBody LegalHoldRequest request) {
        return ResponseEntity.status(201).body(legalHoldService.placeLegalHold(request));
    }

    @DeleteMapping("/{holdId}")
    @PreAuthorize("hasRole('LEGAL_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<LegalHold> release(@PathVariable UUID holdId, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(legalHoldService.releaseLegalHold(holdId, reason));
    }

    @GetMapping
    @PreAuthorize("hasRole('LEGAL_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<List<LegalHold>> listActive(@RequestParam(required = false) String caseReference) {
        if (caseReference == null || caseReference.isBlank()) {
            return ResponseEntity.ok(legalHoldService.getActiveLegalHolds());
        }
        return ResponseEntity.ok(legalHoldService.getDocumentsUnderHold(caseReference));
    }

    @GetMapping("/document/{documentId}")
    @PreAuthorize("hasRole('LEGAL_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<List<LegalHold>> history(@PathVariable UUID documentId) {
        return ResponseEntity.ok(legalHoldService.getLegalHoldHistory(documentId));
    }
}

