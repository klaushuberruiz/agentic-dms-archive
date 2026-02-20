package com.dms.controller;

import com.dms.domain.DocumentType;
import com.dms.service.DocumentTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/document-types")
@RequiredArgsConstructor
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentType> create(@Valid @RequestBody DocumentType request) {
        return ResponseEntity.status(201).body(documentTypeService.createDocumentType(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<org.springframework.data.domain.Page<DocumentType>> list(Pageable pageable) {
        return ResponseEntity.ok(documentTypeService.listDocumentTypes(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentType>> listActive() {
        return ResponseEntity.ok(documentTypeService.getActiveDocumentTypes());
    }

    @GetMapping("/{typeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentType> getById(@PathVariable UUID typeId) {
        return ResponseEntity.ok(documentTypeService.getDocumentType(typeId));
    }

    @PutMapping("/{typeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentType> update(@PathVariable UUID typeId, @Valid @RequestBody DocumentType request) {
        return ResponseEntity.ok(documentTypeService.updateDocumentType(typeId, request));
    }

    @DeleteMapping("/{typeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID typeId) {
        documentTypeService.deactivateDocumentType(typeId);
        return ResponseEntity.noContent().build();
    }
}
