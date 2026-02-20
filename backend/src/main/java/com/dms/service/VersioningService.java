package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.DocumentVersion;
import com.dms.exception.DocumentNotFoundException;
import com.dms.exception.ValidationException;
import com.dms.repository.DocumentRepository;
import com.dms.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersioningService {

	private final DocumentRepository documentRepository;
	private final DocumentVersionRepository documentVersionRepository;
	private final BlobStorageService blobStorageService;
	private final TenantContext tenantContext;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

	@Transactional
	public void uploadNewVersion(UUID documentId, MultipartFile file) {
		UUID tenantId = tenantContext.getCurrentTenantId();

		Document document = documentRepository.findByIdAndTenantId(documentId, tenantId)
			.orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        authorizationService.assertCanAccessDocument(document);

		int newVersion = document.getCurrentVersion() + 1;
		Instant now = Instant.now();

		String blobPath = String.format("%s/%s/%d/%02d/%s_v%d.pdf",
			document.getTenantId(), document.getDocumentType().getName(), now.atZone(java.time.ZoneOffset.UTC).getYear(), now.atZone(java.time.ZoneOffset.UTC).getMonthValue(), document.getId(), newVersion);

		// upload blob
		blobStorageService.uploadBlob(blobPath, file);
		String contentHash = calculateSha256(file);

		// persist version entity
		DocumentVersion version = DocumentVersion.builder()
			.id(UUID.randomUUID())
			.document(document)
			.tenantId(document.getTenantId())
			.versionNumber(newVersion)
			.blobPath(blobPath)
			.fileSizeBytes(file.getSize())
			.contentType(file.getContentType())
			.contentHash(contentHash)
			.createdAt(now)
			.createdBy(tenantContext.getCurrentUserId())
			.build();

		documentVersionRepository.save(version);

		document.setCurrentVersion(newVersion);
		documentRepository.save(document);
        auditService.logMetadataUpdate(document.getId(), java.util.Map.of("version", newVersion - 1), java.util.Map.of("version", newVersion));

		log.info("Uploaded new version {} for document {}", newVersion, documentId);
	}

	@Transactional(readOnly = true)
	public List<DocumentVersion> getVersionHistory(UUID documentId) {
		UUID tenantId = tenantContext.getCurrentTenantId();
		Document document = documentRepository.findByIdAndTenantId(documentId, tenantId)
			.orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        authorizationService.assertCanAccessDocument(document);

		return documentVersionRepository.findAllByDocumentOrderByVersionNumberDesc(document);
	}

	@Transactional(readOnly = true)
	public InputStream getVersionContent(UUID documentId, int versionNumber) {
		UUID tenantId = tenantContext.getCurrentTenantId();
		Document document = documentRepository.findByIdAndTenantId(documentId, tenantId)
			.orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        authorizationService.assertCanAccessDocument(document);

		DocumentVersion v = documentVersionRepository.findByDocumentAndVersionNumber(document, versionNumber)
			.orElseThrow(() -> new DocumentNotFoundException("Version not found"));

		return blobStorageService.downloadBlob(v.getBlobPath());
	}

	@Transactional
	public void restoreVersion(UUID documentId, int versionNumber) {
		UUID tenantId = tenantContext.getCurrentTenantId();
		Document document = documentRepository.findByIdAndTenantId(documentId, tenantId)
			.orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        authorizationService.assertCanAccessDocument(document);

		DocumentVersion previous = documentVersionRepository.findByDocumentAndVersionNumber(document, versionNumber)
			.orElseThrow(() -> new DocumentNotFoundException("Version not found"));

		// create new version that copies blob path content
		int newVersion = document.getCurrentVersion() + 1;
		Instant now = Instant.now();

		String newBlobPath = String.format("%s/%s/%d/%02d/%s_v%d.pdf",
			document.getTenantId(), document.getDocumentType().getName(), now.atZone(java.time.ZoneOffset.UTC).getYear(), now.atZone(java.time.ZoneOffset.UTC).getMonthValue(), document.getId(), newVersion);

		// Copy blob by streaming from previous path to new path
		try (InputStream is = blobStorageService.downloadBlob(previous.getBlobPath())) {
			blobStorageService.uploadBlob(newBlobPath, is, previous.getFileSizeBytes(), previous.getContentType());
		} catch (Exception ex) {
			throw new ValidationException("Failed to restore version");
		}

		DocumentVersion newVer = DocumentVersion.builder()
			.id(UUID.randomUUID())
			.document(document)
			.tenantId(document.getTenantId())
			.versionNumber(newVersion)
			.blobPath(newBlobPath)
			.fileSizeBytes(previous.getFileSizeBytes())
			.contentType(previous.getContentType())
			.contentHash(previous.getContentHash())
			.createdAt(now)
			.createdBy(tenantContext.getCurrentUserId())
			.build();

		documentVersionRepository.save(newVer);
		document.setCurrentVersion(newVersion);
		documentRepository.save(document);
        auditService.logMetadataUpdate(document.getId(), java.util.Map.of("restoredFrom", versionNumber), java.util.Map.of("newVersion", newVersion));
	}

	private String calculateSha256(MultipartFile file) {
		try (InputStream inputStream = file.getInputStream()) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[8192];
			int read;
			while ((read = inputStream.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (IOException | NoSuchAlgorithmException ex) {
			throw new ValidationException("Failed to calculate content hash");
		}
	}
}
