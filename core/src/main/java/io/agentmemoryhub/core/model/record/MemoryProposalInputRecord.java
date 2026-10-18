package io.agentmemoryhub.core.model.record;

import java.util.List;

public record MemoryProposalInputRecord(
        String proposedContent,
        String reason,
        List<String> tags
) {}
