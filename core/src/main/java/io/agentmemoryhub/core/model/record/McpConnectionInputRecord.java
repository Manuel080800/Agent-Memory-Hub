package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ConnectionType;

public record McpConnectionInputRecord(
        String name,
        ConnectionType connectionType,
        Boolean enabled
) {}
