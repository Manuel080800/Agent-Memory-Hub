package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ApplicationMode;
import java.util.List;
import java.time.Instant;

public record SkillRecord(
        String id,
        String profileId,
        String name,
        String description,
        String instructions,
        List<String> tags,
        ApplicationMode applicationMode,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
