package io.agentmemoryhub.core.model.record;

import java.time.Instant;

public record TaskContextTaskRecord(
        String id,
        String profileId,
        String title,
        String description,
        String summary,
        Instant createdAt,
        Instant updatedAt,
        long version,
        boolean truncated,
        int estimatedTokens,
        String fullReadTool
) {}
