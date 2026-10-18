package io.agentmemoryhub.core.model.record;


public record ProfileInputRecord(
        String name,
        String description,
        Boolean enabled
) {}
