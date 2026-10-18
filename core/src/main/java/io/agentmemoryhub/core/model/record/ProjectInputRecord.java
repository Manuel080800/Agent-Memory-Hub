package io.agentmemoryhub.core.model.record;


public record ProjectInputRecord(
        String name,
        String description,
        Boolean enabled,
        String ownerUserId
) {}
