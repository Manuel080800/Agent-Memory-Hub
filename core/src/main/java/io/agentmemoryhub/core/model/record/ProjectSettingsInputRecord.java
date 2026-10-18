package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.TaskCreationMode;

public record ProjectSettingsInputRecord(
        Boolean contextRetrievalEnabled,
        Boolean compactRecoveryEnabled,
        Boolean contextualSelectionEnabled,
        TaskCreationMode taskCreationMode,
        Boolean automaticSummaryEnabled,
        Boolean selectedTaskRetrievalEnabled,
        Integer contextBudgetTokens,
        Integer permanentQuotaPercent,
        Boolean preToolUseContextEnabled
) {}
