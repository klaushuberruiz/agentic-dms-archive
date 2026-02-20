package com.dms.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class BlobStorageService {

    private final BlobContainerClient blobContainerClient;
    private final Path localStorageRoot;

    public BlobStorageService(
            ObjectProvider<BlobContainerClient> blobContainerClientProvider,
            @Value("${dms.local-storage.path:./target/local-blob-storage}") String localStoragePath) {
        this.blobContainerClient = blobContainerClientProvider.getIfAvailable();
        this.localStorageRoot = Path.of(localStoragePath).toAbsolutePath().normalize();
    }
    
    public void uploadBlob(String blobPath, MultipartFile file) {
        try {
            if (blobContainerClient != null) {
                BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
                blobClient.upload(file.getInputStream(), file.getSize(), true);
            } else {
                Path target = resolveLocalPath(blobPath);
                Files.createDirectories(target.getParent());
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            log.info("Uploaded blob: {}", blobPath);
        } catch (IOException e) {
            log.error("Failed to upload blob: {}", blobPath, e);
            throw new RuntimeException("Blob upload failed", e);
        }
    }
    
    public InputStream downloadBlob(String blobPath) {
        try {
            if (blobContainerClient != null) {
                BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
                return blobClient.openInputStream();
            }
            Path source = resolveLocalPath(blobPath);
            return Files.newInputStream(source);
        } catch (IOException e) {
            log.error("Failed to download blob: {}", blobPath, e);
            return new ByteArrayInputStream(new byte[0]);
        }
    }
    
    public void deleteBlob(String blobPath) {
        if (blobContainerClient != null) {
            BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
            blobClient.delete();
        } else {
            try {
                Files.deleteIfExists(resolveLocalPath(blobPath));
            } catch (IOException e) {
                log.error("Failed to delete local blob: {}", blobPath, e);
                throw new RuntimeException("Blob delete failed", e);
            }
        }
        log.info("Deleted blob: {}", blobPath);
    }

    public void uploadBlob(String blobPath, java.io.InputStream content, long size, String contentType) {
        try {
            if (blobContainerClient != null) {
                BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
                blobClient.upload(content, size, true);
                if (contentType != null) {
                    blobClient.setHttpHeaders(new com.azure.storage.blob.models.BlobHttpHeaders().setContentType(contentType));
                }
            } else {
                Path target = resolveLocalPath(blobPath);
                Files.createDirectories(target.getParent());
                Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Uploaded blob (stream): {}", blobPath);
        } catch (Exception e) {
            log.error("Failed to upload blob (stream): {}", blobPath, e);
            throw new RuntimeException("Blob upload failed", e);
        }
    }

    private Path resolveLocalPath(String blobPath) {
        return localStorageRoot.resolve(blobPath).normalize();
    }
}
