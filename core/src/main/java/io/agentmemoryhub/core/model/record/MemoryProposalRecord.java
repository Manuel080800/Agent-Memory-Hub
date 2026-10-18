package io.agentmemoryhub.core.model.record;

import java.util.List;
import java.time.Instant;

public record MemoryProposalRecord(
        String id,
        String taskId,
        String proposedContent,
        String reason,
        List<String> tags,
        boolean processed,
        String resultMemoryId,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
