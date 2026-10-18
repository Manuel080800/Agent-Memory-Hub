package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ApplicationMode;
import java.util.List;
import java.time.Instant;

public record MemoryRecord(
        String id,
        String profileId,
        String title,
        String content,
        List<String> tags,
        ApplicationMode applicationMode,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
