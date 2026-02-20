package com.dms.service;

import com.dms.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlobStorageServiceTest {

    @Test
    void uploadValidatedPdfReturnsHash() {
        BlobStorageService service = new BlobStorageService(emptyProvider(), "./target/test-blob-storage", 104857600L, 60, 5);
        MockMultipartFile pdf = new MockMultipartFile(
            "file",
            "sample.pdf",
            "application/pdf",
            "%PDF-1.7\nhello".getBytes(StandardCharsets.UTF_8)
        );

        String hash = service.uploadValidatedPdf("tenant/a.pdf", pdf);

        assertEquals(64, hash.length());
    }

    @Test
    void rejectsNonPdfFile() {
        BlobStorageService service = new BlobStorageService(emptyProvider(), "./target/test-blob-storage", 104857600L, 60, 5);
        MockMultipartFile txt = new MockMultipartFile(
            "file",
            "sample.txt",
            "text/plain",
            "hello".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(ValidationException.class, () -> service.uploadValidatedPdf("tenant/a.txt", txt));
    }

    private ObjectProvider<com.azure.storage.blob.BlobContainerClient> emptyProvider() {
        return new ObjectProvider<>() {
            @Override
            public com.azure.storage.blob.BlobContainerClient getObject(Object... args) {
                return null;
            }

            @Override
            public com.azure.storage.blob.BlobContainerClient getIfAvailable() {
                return null;
            }

            @Override
            public com.azure.storage.blob.BlobContainerClient getIfUnique() {
                return null;
            }

            @Override
            public com.azure.storage.blob.BlobContainerClient getObject() {
                return null;
            }
        };
    }
}
