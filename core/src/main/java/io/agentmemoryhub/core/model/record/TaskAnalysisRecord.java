package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.TaskCreationMode;

public record TaskAnalysisRecord(
        TaskCreationMode mode,
        String complexity,
        boolean shouldSuggest,
        boolean shouldCreateAutomatically,
        String reason,
        String suggestedTitle,
        String suggestedDescription
) {}
