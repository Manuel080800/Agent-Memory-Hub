package io.agentmemoryhub.core.model.record;

import java.time.Instant;

public record TaskRecord(
        String id,
        String profileId,
        String title,
        String description,
        String summary,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
