package io.agentmemoryhub.core.model.record;

import java.time.Instant;

public record ProjectRecord(
        String id,
        String name,
        String description,
        boolean enabled,
        String ownerUserId,
        String activeProfileId,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
