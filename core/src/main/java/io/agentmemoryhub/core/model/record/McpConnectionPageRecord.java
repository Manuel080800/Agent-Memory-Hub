package io.agentmemoryhub.core.model.record;

import java.util.List;

public record McpConnectionPageRecord(
        List<McpConnectionRecord> items,
        int page,
        int pageSize,
        long total
) {}
