package io.agentmemoryhub.core.model.record;

public record TaskContinuityRecord(
        String projectId,
        String activeProfileId,
        TaskRecord task,
        boolean valid,
        String incompatibilityReason,
        int blockCount
) {}
