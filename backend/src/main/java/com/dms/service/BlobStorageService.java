package com.dms.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlobStorageService {
    
    private final BlobContainerClient blobContainerClient;
    
    public void uploadBlob(String blobPath, MultipartFile file) {
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            log.info("Uploaded blob: {}", blobPath);
        } catch (IOException e) {
            log.error("Failed to upload blob: {}", blobPath, e);
            throw new RuntimeException("Blob upload failed", e);
        }
    }
    
    public InputStream downloadBlob(String blobPath) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
        return blobClient.openInputStream();
    }
    
    public void deleteBlob(String blobPath) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
        blobClient.delete();
        log.info("Deleted blob: {}", blobPath);
    }
}
