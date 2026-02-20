package com.dms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    
    @Value("${azure.openai.endpoint}")
    private String azureOpenAiEndpoint;
    
    @Value("${azure.openai.api-key}")
    private String azureOpenAiKey;
    
    @Value("${azure.openai.deployment-id}")
    private String deploymentId;
    
    private final RestTemplate restTemplate;
    
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text for embedding cannot be null or empty");
        }
        
        if (text.length() > 8000) {
            text = text.substring(0, 8000);
        }
        
        try {
            log.debug("Generating embedding for text of length: {}", text.length());
            
            // In production, call Azure OpenAI embedding API
            // For now, return mock embedding vector of 1536 dimensions (ada-002 size)
            return generateMockEmbedding();
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
    
    public List<List<Double>> generateEmbeddings(List<String> texts) {
        return texts.stream()
            .map(this::generateEmbedding)
            .toList();
    }
    
    private List<Double> generateMockEmbedding() {
        // Return mock 1536-dimensional vector for testing
        double[] values = new double[1536];
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.random();
        }
        return java.util.Arrays.stream(values).boxed().toList();
    }
}
