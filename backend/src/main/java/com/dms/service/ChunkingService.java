package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.RequirementChunk;
import com.dms.dto.response.ChunkMetrics;
import com.dms.exception.DocumentNotFoundException;
import com.dms.repository.DocumentRepository;
import com.dms.repository.RequirementChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkingService {
    
    private final DocumentRepository documentRepository;
    private final RequirementChunkRepository requirementChunkRepository;
    private final BlobStorageService blobStorageService;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    
    private static final int MAX_CHUNK_TOKENS = 1000;
    private static final int AVERAGE_TOKENS_PER_WORD = 1; // Approximate
    private static final int WORDS_PER_CHUNK = MAX_CHUNK_TOKENS / AVERAGE_TOKENS_PER_WORD;
    
    @Async
    @Transactional
    public void processDocumentAsync(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        
        try {
            log.info("Starting async chunking process: documentId={}, tenant={}", documentId, tenantId);
            
            Document document = documentRepository
                .findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
            
            // Download blob using blob path
            InputStream blobStream = blobStorageService.downloadBlob(document.getBlobPath());
            
            // Extract text based on file type
            String fileName = (String) document.getMetadata().getOrDefault("fileName", "document.pdf");
            String extractedText = extractTextFromInputStream(blobStream, fileName);
            
            // Create chunks
            List<RequirementChunk> chunks = createChunks(document, extractedText);
            
            // Persist chunks
            for (RequirementChunk chunk : chunks) {
                requirementChunkRepository.save(chunk);
            }
            
            // Update document chunking status
            document.getMetadata().put("chunkedAt", Instant.now().toString());
            document.getMetadata().put("chunkCount", chunks.size());
            documentRepository.save(document);
            
            log.info("Chunking completed: documentId={}, chunkCount={}", documentId, chunks.size());
        } catch (Exception e) {
            log.error("Failed to chunk document: {}", documentId, e);
        }
    }
    
    @Transactional
    public ChunkMetrics getChunkingMetrics(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        
        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        
        int chunkCount = (Integer) document.getMetadata().getOrDefault("chunkCount", 0);
        String chunkedAt = (String) document.getMetadata().get("chunkedAt");
        
        List<RequirementChunk> chunks = requirementChunkRepository.findByDocumentId(documentId);
        
        return ChunkMetrics.builder()
            .documentId(documentId)
            .chunkCount(chunks.size())
            .totalTokens(chunks.stream().mapToInt(c -> c.getTokenCount()).sum())
            .averageChunkSize(chunks.isEmpty() ? 0 : chunks.stream().mapToInt(c -> c.getChunkText().length()).sum() / chunks.size())
            .chunkedAt(chunkedAt != null ? Instant.parse(chunkedAt) : null)
            .build();
    }
    
    @Transactional(readOnly = true)
    public List<RequirementChunk> getDocumentChunks(UUID documentId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        
        Document document = documentRepository
            .findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        
        return requirementChunkRepository.findByDocumentId(documentId);
    }
    
    private String extractTextFromInputStream(InputStream inputStream, String fileName) throws IOException, org.apache.tika.exception.TikaException, org.xml.sax.SAXException {
        try (inputStream) {
            Tika tika = new Tika();
            return tika.parseToString(inputStream);
        }
    }
    
    private List<RequirementChunk> createChunks(Document document, String text) {
        List<RequirementChunk> chunks = new ArrayList<>();
        
        // Split by sentences initially
        String[] sentences = text.split("[.!?]");
        
        List<String> currentChunk = new ArrayList<>();
        int currentTokenCount = 0;
        int sequenceNumber = 1;
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            int sentenceTokenCount = estimateTokenCount(sentence);
            
            if (currentTokenCount + sentenceTokenCount > MAX_CHUNK_TOKENS && !currentChunk.isEmpty()) {
                // Save current chunk and start new one
                RequirementChunk chunk = createChunk(document, currentChunk, currentTokenCount, sequenceNumber);
                chunks.add(chunk);
                
                currentChunk.clear();
                currentTokenCount = 0;
                sequenceNumber++;
            }
            
            currentChunk.add(sentence);
            currentTokenCount += sentenceTokenCount;
        }
        
        // Save remaining chunk
        if (!currentChunk.isEmpty()) {
            RequirementChunk chunk = createChunk(document, currentChunk, currentTokenCount, sequenceNumber);
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    private RequirementChunk createChunk(Document document, List<String> sentences, int tokenCount, int sequence) {
        String content = String.join(" ", sentences);
        UUID chunkId = UUID.randomUUID();
        
        return RequirementChunk.builder()
            .id(chunkId)
            .tenantId(document.getTenantId())
            .documentId(document.getId())
            .chunkOrder(sequence)
            .chunkText(content)
            .tokenCount(tokenCount)
            .createdAt(Instant.now())
            .build();
    }
    
    private int estimateTokenCount(String text) {
        // Simple estimation: ~4 characters per token on average
        // More sophisticated: use actual tokenizer
        return Math.max(1, text.length() / 4);
    }
}
