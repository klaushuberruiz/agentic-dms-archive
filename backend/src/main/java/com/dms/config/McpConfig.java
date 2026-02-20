package com.dms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class McpConfig {

    @Bean
    public Set<String> mcpAllowedTools() {
        return Set.of(
            "search_documents",
            "get_document",
            "search_requirements",
            "get_requirement_by_id",
            "get_related_requirements",
            "validate_requirement_references"
        );
    }
}
