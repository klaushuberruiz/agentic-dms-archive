package com.dms.service;

import com.dms.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataValidationService {
    
    private final ObjectMapper objectMapper;
    
    public void validate(Map<String, Object> metadata, Map<String, Object> schemaMap) {
        try {
            JsonNode metadataNode = objectMapper.valueToTree(metadata);
            JsonNode schemaNode = objectMapper.valueToTree(schemaMap);
            
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema schema = factory.getSchema(schemaNode);
            
            Set<ValidationMessage> errors = schema.validate(metadataNode);
            
            if (!errors.isEmpty()) {
                String errorMessage = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
                throw new ValidationException("Metadata validation failed: " + errorMessage);
            }
        } catch (Exception e) {
            log.error("Metadata validation error", e);
            throw new ValidationException("Metadata validation failed: " + e.getMessage());
        }
    }
}
