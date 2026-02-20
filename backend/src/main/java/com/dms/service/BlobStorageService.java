package com.dms.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.dms.exception.BlobStorageException;
import com.dms.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class BlobStorageService {

    private static final int PDF_MAGIC_BYTES_LENGTH = 5;
    private static final byte[] PDF_MAGIC_BYTES = new byte[] { '%', 'P', 'D', 'F', '-' };

    private final BlobContainerClient blobContainerClient;
    private final Path localStorageRoot;
    private final long maxUploadBytes;
    private final Duration circuitOpenDuration;
    private final int circuitFailureThreshold;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile long circuitOpenUntilEpochMillis = 0;

    public BlobStorageService(
            ObjectProvider<BlobContainerClient> blobContainerClientProvider,
            @Value("${dms.local-storage.path:./target/local-blob-storage}") String localStoragePath,
            @Value("${dms.blob.max-upload-bytes:104857600}") long maxUploadBytes,
            @Value("${dms.resilience.circuit-breaker.open-duration-seconds:60}") long openDurationSeconds,
            @Value("${dms.resilience.circuit-breaker.failure-threshold:5}") int circuitFailureThreshold) {
        this.blobContainerClient = blobContainerClientProvider.getIfAvailable();
        this.localStorageRoot = Path.of(localStoragePath).toAbsolutePath().normalize();
        this.maxUploadBytes = maxUploadBytes;
        this.circuitOpenDuration = Duration.ofSeconds(Math.max(1, openDurationSeconds));
        this.circuitFailureThreshold = Math.max(1, circuitFailureThreshold);
    }

    public void uploadBlob(String blobPath, MultipartFile file) {
        try {
            ValidatedBlob validated = validatePdfAndCalculateHash(file);
            uploadBlob(blobPath, new ByteArrayInputStream(validated.bytes()), validated.size(), file.getContentType());
        } catch (IOException e) {
            throw new BlobStorageException("Blob upload failed", e);
        }
    }

    public String uploadValidatedPdf(String blobPath, MultipartFile file) {
        try {
            ValidatedBlob validated = validatePdfAndCalculateHash(file);
            uploadBlob(blobPath, new ByteArrayInputStream(validated.bytes()), validated.size(), file.getContentType());
            return validated.sha256();
        } catch (IOException e) {
            throw new BlobStorageException("Blob upload failed", e);
        }
    }

    public String calculateContentHash(MultipartFile file) {
        try {
            return validatePdfAndCalculateHash(file).sha256();
        } catch (IOException e) {
            throw new BlobStorageException("Failed to hash content", e);
        }
    }

    public InputStream downloadBlob(String blobPath) {
        return runWithCircuitBreaker(() -> {
            if (blobContainerClient != null) {
                BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
                return blobClient.openInputStream();
            }
            try {
                Path source = resolveLocalPath(blobPath);
                return Files.newInputStream(source);
            } catch (IOException e) {
                throw new BlobStorageException("Blob download failed", e);
            }
        });
    }

    public byte[] downloadBlobRange(String blobPath, long startInclusive, long endInclusive) {
        if (startInclusive < 0 || endInclusive < startInclusive) {
            throw new ValidationException("Invalid byte range");
        }
        try (InputStream in = downloadBlob(blobPath)) {
            long skipped = in.skip(startInclusive);
            while (skipped < startInclusive) {
                long delta = in.skip(startInclusive - skipped);
                if (delta <= 0) {
                    return new byte[0];
                }
                skipped += delta;
            }
            long remaining = endInclusive - startInclusive + 1;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new BlobStorageException("Blob ranged download failed", e);
        }
    }

    public void deleteBlob(String blobPath) {
        runWithCircuitBreaker(() -> {
            if (blobContainerClient != null) {
                BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
                blobClient.deleteIfExists();
            } else {
                try {
                    Files.deleteIfExists(resolveLocalPath(blobPath));
                } catch (IOException e) {
                    throw new BlobStorageException("Blob delete failed", e);
                }
            }
            log.info("Deleted blob: {}", blobPath);
            return null;
        });
    }

    public void uploadBlob(String blobPath, java.io.InputStream content, long size, String contentType) {
        runWithCircuitBreaker(() -> {
            if (blobContainerClient != null) {
                BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
                blobClient.upload(content, size, true);
                if (contentType != null) {
                    blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
                }
            } else {
                try {
                    Path target = resolveLocalPath(blobPath);
                    Files.createDirectories(target.getParent());
                    Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BlobStorageException("Blob upload failed", e);
                }
            }
            log.info("Uploaded blob (stream): {}", blobPath);
            return null;
        });
    }

    public String generateReadSasUrl(String blobPath, Duration duration) {
        Duration effectiveDuration = duration == null || duration.isNegative() || duration.isZero()
            ? Duration.ofMinutes(60)
            : duration;
        if (blobContainerClient != null) {
            BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
            BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
            OffsetDateTime expiresAt = OffsetDateTime.now().plus(effectiveDuration);
            BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiresAt, permissions);
            return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
        }
        return "local://" + resolveLocalPath(blobPath) + "?expiresAt=" + OffsetDateTime.now().plus(effectiveDuration);
    }

    private Path resolveLocalPath(String blobPath) {
        return localStorageRoot.resolve(blobPath).normalize();
    }

    private <T> T runWithCircuitBreaker(SupplierWithException<T> operation) {
        long now = System.currentTimeMillis();
        if (now < circuitOpenUntilEpochMillis) {
            throw new BlobStorageException("Blob storage circuit breaker is OPEN");
        }
        try {
            T result = operation.run();
            consecutiveFailures.set(0);
            return result;
        } catch (Exception ex) {
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= circuitFailureThreshold) {
                circuitOpenUntilEpochMillis = System.currentTimeMillis() + circuitOpenDuration.toMillis();
            }
            if (ex instanceof BlobStorageException bse) {
                throw bse;
            }
            throw new BlobStorageException("Blob storage operation failed", ex);
        }
    }

    private ValidatedBlob validatePdfAndCalculateHash(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new ValidationException("File exceeds maximum size of 100MB");
        }
        byte[] bytes = file.getBytes();
        if (bytes.length < PDF_MAGIC_BYTES_LENGTH) {
            throw new ValidationException("Invalid PDF file");
        }
        for (int i = 0; i < PDF_MAGIC_BYTES_LENGTH; i++) {
            if (bytes[i] != PDF_MAGIC_BYTES[i]) {
                throw new ValidationException("Uploaded file must be a PDF");
            }
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String sha256 = HexFormat.of().formatHex(digest.digest(bytes));
            return new ValidatedBlob(bytes, sha256, bytes.length);
        } catch (NoSuchAlgorithmException e) {
            throw new BlobStorageException("SHA-256 algorithm unavailable", e);
        }
    }

    private record ValidatedBlob(byte[] bytes, String sha256, long size) {
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T run() throws Exception;
    }
}
