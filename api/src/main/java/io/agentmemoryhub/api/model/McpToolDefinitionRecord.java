package io.agentmemoryhub.api.model;

import java.util.Map;

public record McpToolDefinitionRecord(
        String name,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        Map<String, Object> annotations
) {}
