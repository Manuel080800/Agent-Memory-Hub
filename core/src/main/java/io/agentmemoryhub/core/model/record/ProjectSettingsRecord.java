package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.TaskCreationMode;
import java.time.Instant;

public record ProjectSettingsRecord(
        String projectId,
        boolean contextRetrievalEnabled,
        boolean compactRecoveryEnabled,
        boolean contextualSelectionEnabled,
        TaskCreationMode taskCreationMode,
        boolean automaticSummaryEnabled,
        boolean selectedTaskRetrievalEnabled,
        int contextBudgetTokens,
        int permanentQuotaPercent,
        boolean preToolUseContextEnabled,
        Instant updatedAt,
        long version
) {}
