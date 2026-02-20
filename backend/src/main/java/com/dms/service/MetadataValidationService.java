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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataValidationService {

    private final ObjectMapper objectMapper;

    public void validate(Map<String, Object> metadata, Map<String, Object> schemaMap) {
        validateWithSchemaVersion(metadata, schemaMap, null);
    }

    public void validateWithSchemaVersion(Map<String, Object> metadata, Map<String, Object> schemaMap, String expectedSchemaVersion) {
        try {
            JsonNode metadataNode = objectMapper.valueToTree(metadata == null ? Map.of() : metadata);
            JsonNode schemaNode = objectMapper.valueToTree(schemaMap == null ? Map.of() : schemaMap);

            if (expectedSchemaVersion != null && !expectedSchemaVersion.isBlank()) {
                JsonNode versionNode = schemaNode.get("schemaVersion");
                if (versionNode != null && !expectedSchemaVersion.equals(versionNode.asText())) {
                    throw new ValidationException("Metadata schema version mismatch. Expected " + expectedSchemaVersion + " but was " + versionNode.asText());
                }
            }

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema schema = factory.getSchema(schemaNode);
            Set<ValidationMessage> errors = schema.validate(metadataNode);

            if (!errors.isEmpty()) {
                Map<String, String> fieldErrors = buildFieldErrors(errors);
                String errorMessage = fieldErrors.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("; "));
                throw new ValidationException("Metadata validation failed: " + errorMessage);
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Metadata validation error", e);
            throw new ValidationException("Metadata validation failed: " + e.getMessage());
        }
    }

    private Map<String, String> buildFieldErrors(Set<ValidationMessage> errors) {
        return errors.stream()
            .sorted(Comparator.comparing(ValidationMessage::getPath))
            .collect(Collectors.toMap(
                this::normalizePath,
                ValidationMessage::getMessage,
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private String normalizePath(ValidationMessage message) {
        String path = message.getPath();
        if (path == null || path.isBlank() || "$".equals(path)) {
            return "metadata";
        }
        return path.replace("$.", "");
    }
}
