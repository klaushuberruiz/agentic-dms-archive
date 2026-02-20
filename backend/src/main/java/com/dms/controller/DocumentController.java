package com.dms.controller;

import com.dms.dto.request.DocumentUploadRequest;
import com.dms.dto.request.BulkDownloadRequest;
import com.dms.dto.request.MetadataUpdateRequest;
import com.dms.dto.response.DocumentResponse;
import com.dms.dto.response.VersionHistoryResponse;
import com.dms.domain.DocumentVersion;
import com.dms.service.DocumentService;
import com.dms.service.SearchService;
import com.dms.service.VersioningService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpRange;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Validated
public class DocumentController {

	private final DocumentService documentService;
	private final VersioningService versioningService;
    private final SearchService searchService;

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<DocumentResponse> uploadDocument(
			@Valid @RequestPart("request") DocumentUploadRequest request,
			@RequestPart("file") MultipartFile file) {
		DocumentResponse resp = documentService.uploadDocument(request, file);
		return ResponseEntity.status(201).body(resp);
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@GetMapping
	public ResponseEntity<Page<DocumentResponse>> listDocuments(Pageable pageable) {
		return ResponseEntity.ok(documentService.listDocuments(pageable));
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@GetMapping("/{id}")
	public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
		DocumentResponse resp = documentService.getDocument(id);
		return ResponseEntity.ok(resp);
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@GetMapping("/{id}/download")
	public ResponseEntity<?> download(@PathVariable UUID id, @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
		DocumentResponse meta = documentService.getDocument(id);
		long fileSize = meta.getFileSizeBytes() != null ? meta.getFileSizeBytes() : 0;
		if (range != null && !range.isBlank() && fileSize > 0) {
			List<HttpRange> ranges = HttpRange.parseRanges(range);
			if (!ranges.isEmpty()) {
				HttpRange first = ranges.get(0);
				long start = first.getRangeStart(fileSize);
				long end = first.getRangeEnd(fileSize);
				byte[] bytes = documentService.downloadDocumentRange(id, start, end);
				return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
					.header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
					.header(HttpHeaders.ACCEPT_RANGES, "bytes")
					.contentLength(bytes.length)
					.contentType(MediaType.APPLICATION_PDF)
					.body(bytes);
			}
		}
		InputStreamResource resource = new InputStreamResource(documentService.downloadDocument(id));

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".pdf\"")
			.header(HttpHeaders.ACCEPT_RANGES, "bytes")
			.contentLength(fileSize)
			.contentType(MediaType.APPLICATION_PDF)
			.body(resource);
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@GetMapping("/{id}/preview")
	public ResponseEntity<InputStreamResource> preview(@PathVariable UUID id) {
		DocumentResponse meta = documentService.getDocument(id);
		InputStream is = documentService.previewDocument(id);
		InputStreamResource resource = new InputStreamResource(is);

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + id + ".pdf\"")
			.contentLength(meta.getFileSizeBytes() != null ? meta.getFileSizeBytes() : 0)
			.contentType(MediaType.APPLICATION_PDF)
			.body(resource);
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@GetMapping("/{id}/download-url")
	public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable UUID id) {
		return ResponseEntity.ok(Map.of("url", documentService.generateDownloadUrl(id)));
	}

	@PutMapping("/{id}/metadata")
	@PreAuthorize("hasRole('DOCUMENT_USER')")
	public ResponseEntity<DocumentResponse> updateMetadata(@PathVariable UUID id,
			@Valid @RequestBody MetadataUpdateRequest request) {
		DocumentResponse resp = documentService.updateMetadata(id, request.getMetadata());
		return ResponseEntity.ok(resp);
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> softDelete(@PathVariable UUID id, @RequestParam(required = false) String reason) {
		documentService.softDeleteDocument(id, reason == null ? "" : reason);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@PostMapping("/{id}/restore")
	public ResponseEntity<Void> restore(@PathVariable UUID id) {
		documentService.restoreDocument(id);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{id}/hard")
	public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
		documentService.hardDeleteDocument(id);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@GetMapping("/{id}/versions")
	public ResponseEntity<java.util.List<VersionHistoryResponse>> getVersions(@PathVariable UUID id) {
		java.util.List<DocumentVersion> versions = versioningService.getVersionHistory(id);
		java.util.List<VersionHistoryResponse> response = versions.stream()
			.sorted(Comparator.comparing(DocumentVersion::getVersionNumber).reversed())
			.map(version -> VersionHistoryResponse.builder()
				.documentId(id)
				.versionNumber(version.getVersionNumber())
				.blobPath(version.getBlobPath())
				.fileSizeBytes(version.getFileSizeBytes())
				.contentHash(version.getContentHash())
				.createdBy(version.getCreatedBy())
				.createdAt(version.getCreatedAt())
				.build())
			.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> uploadVersion(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
		versioningService.uploadNewVersion(id, file);
		return ResponseEntity.status(201).build();
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@GetMapping("/{id}/versions/{version}")
	public ResponseEntity<InputStreamResource> getVersion(@PathVariable UUID id, @PathVariable int version) {
		InputStream is = versioningService.getVersionContent(id, version);
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.body(new InputStreamResource(is));
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@PostMapping("/{id}/versions/{version}/restore")
	public ResponseEntity<Void> restoreVersion(@PathVariable UUID id, @PathVariable int version) {
		versioningService.restoreVersion(id, version);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('DOCUMENT_USER')")
	@PostMapping("/bulk-download")
	public ResponseEntity<byte[]> bulkDownload(@Valid @RequestBody BulkDownloadRequest request) {
		byte[] archive = searchService.bulkDownload(request.getDocumentIds());
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documents.zip\"")
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.body(archive);
	}
}
