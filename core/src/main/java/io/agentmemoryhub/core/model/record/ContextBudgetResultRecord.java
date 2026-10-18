package io.agentmemoryhub.core.model.record;

import java.util.List;

public record ContextBudgetResultRecord<T>(
        List<T> resourceList,
        int estimatedTokens,
        int omittedCount,
        boolean truncated
) {}
