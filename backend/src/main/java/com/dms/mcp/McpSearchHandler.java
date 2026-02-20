package com.dms.mcp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class McpSearchHandler {

    private final McpToolHandler mcpToolHandler;

    public List<Map<String, Object>> search(String query, int limit) {
        return mcpToolHandler.searchRequirements(query, limit);
    }
}
