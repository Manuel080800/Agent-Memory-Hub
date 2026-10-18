package io.agentmemoryhub.core.model.record;

import java.time.Instant;

public record ProfileRecord(
        String id,
        String projectId,
        String name,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
