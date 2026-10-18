package io.agentmemoryhub.core.model.record;

import java.util.List;

public record ProjectContextRecord(
        String projectId,
        String activeProfileId,
        List<ContextResourceRecord> workflows,
        List<ContextResourceRecord> skills,
        List<ContextResourceRecord> memories,
        int estimatedTokens,
        boolean truncated,
        int omittedCount,
        int permanentBudgetTokens
) {}
