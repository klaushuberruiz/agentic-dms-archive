package com.dms.service;

import com.dms.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MetadataValidationProperties {

    private final MetadataValidationService service = new MetadataValidationService(new ObjectMapper());

    @Property
    void missingRequiredFieldAlwaysFails(@ForAll String value) {
        Map<String, Object> schema = Map.of(
            "$schema", "http://json-schema.org/draft-07/schema#",
            "type", "object",
            "required", java.util.List.of("title"),
            "properties", Map.of("title", Map.of("type", "string"))
        );

        Map<String, Object> metadata = Map.of("other", value);

        assertThrows(ValidationException.class, () -> service.validate(metadata, schema));
    }
}
