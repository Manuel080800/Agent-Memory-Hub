package io.agentmemoryhub.api.model;

import java.util.List;

public record McpToolResultRecord(
        List<McpTextContentRecord> content,
        boolean isError,
        Object structuredContent
) {}
