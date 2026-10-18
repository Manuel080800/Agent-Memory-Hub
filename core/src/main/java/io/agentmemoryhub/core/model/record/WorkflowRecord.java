package io.agentmemoryhub.core.model.record;

import java.time.Instant;

public record WorkflowRecord(
        String id,
        String profileId,
        String name,
        String description,
        String instructions,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
