package com.dms.service;

import com.dms.exception.ValidationException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BlobStorageServiceProperties {

    private final BlobStorageService service = new BlobStorageService(emptyProvider(), "./target/test-blob-storage", 1024 * 1024, 60, 5);

    @Property
    void nonPdfPrefixIsRejected(@ForAll String text) {
        String payload = text.startsWith("%PDF-") ? "X" + text : text;
        MockMultipartFile file = new MockMultipartFile("file", "x.bin", "application/octet-stream", payload.getBytes(StandardCharsets.UTF_8));
        assertThrows(ValidationException.class, () -> service.uploadValidatedPdf("tenant/nonpdf.bin", file));
    }

    private ObjectProvider<com.azure.storage.blob.BlobContainerClient> emptyProvider() {
        return new ObjectProvider<>() {
            @Override
            public com.azure.storage.blob.BlobContainerClient getObject(Object... args) { return null; }
            @Override
            public com.azure.storage.blob.BlobContainerClient getIfAvailable() { return null; }
            @Override
            public com.azure.storage.blob.BlobContainerClient getIfUnique() { return null; }
            @Override
            public com.azure.storage.blob.BlobContainerClient getObject() { return null; }
        };
    }
}
