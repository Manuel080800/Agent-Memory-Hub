package io.agentmemoryhub.core.model.record;

import java.util.List;

public record TaskCreationResultRecord(
        TaskRecord task,
        String taskId,
        List<String> nextSteps
) {}
