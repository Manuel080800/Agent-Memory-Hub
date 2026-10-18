package io.agentmemoryhub.core.model.record;

import java.util.List;

public record TaskContextRecord(
        TaskContextTaskRecord task,
        List<TaskContextBlockRecord> blocks,
        boolean validForActiveProfile,
        int estimatedTokens,
        boolean truncated,
        int omittedCount,
        int taskBudgetTokens
) {}
