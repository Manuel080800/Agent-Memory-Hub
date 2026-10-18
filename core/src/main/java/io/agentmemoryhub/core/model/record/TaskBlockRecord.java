package io.agentmemoryhub.core.model.record;

import java.time.Instant;

public record TaskBlockRecord(
        String id,
        String taskId,
        String content,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
