package io.agentmemoryhub.core.model.record;

public record WorkflowInputRecord(
        String name,
        String description,
        String instructions,
        Boolean enabled
) {}
