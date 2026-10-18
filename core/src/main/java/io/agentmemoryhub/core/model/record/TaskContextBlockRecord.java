package io.agentmemoryhub.core.model.record;

import java.time.Instant;

public record TaskContextBlockRecord(
        String id,
        String taskId,
        String content,
        Instant createdAt,
        Instant updatedAt,
        long version,
        boolean truncated,
        int estimatedTokens,
        String fullReadTool
) {}
