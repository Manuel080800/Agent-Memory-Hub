package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ConnectionType;
import java.time.Instant;

public record McpConnectionRecord(
        String id,
        String projectId,
        String name,
        ConnectionType connectionType,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
