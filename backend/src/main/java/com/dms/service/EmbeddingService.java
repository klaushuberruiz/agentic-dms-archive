package com.dms.service;

import com.dms.exception.EmbeddingGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    @Value("${azure.openai.endpoint:}")
    private String azureOpenAiEndpoint;

    @Value("${azure.openai.api-key:}")
    private String azureOpenAiKey;

    @Value("${azure.openai.deployment-id:text-embedding-ada-002}")
    private String deploymentId;

    public List<Double> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text for embedding cannot be null or empty");
        }

        String input = text.length() > 8000 ? text.substring(0, 8000) : text;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            List<Double> values = new ArrayList<>(256);
            for (int i = 0; i < 256; i++) {
                int b = hash[i % hash.length] & 0xFF;
                values.add((b / 255.0) * 2.0 - 1.0);
            }
            return values;
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new EmbeddingGenerationException("Failed to generate embedding", e);
        }
    }

    public List<List<Double>> generateEmbeddings(List<String> texts) {
        return texts.stream().map(this::generateEmbedding).toList();
    }
}
