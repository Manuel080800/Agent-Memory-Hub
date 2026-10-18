package io.agentmemoryhub.core.model.record;

public record IdempotencyKeyRecord(
        String contentHash,
        String resourceId
) {}
