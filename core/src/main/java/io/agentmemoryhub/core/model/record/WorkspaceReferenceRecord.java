package io.agentmemoryhub.core.model.record;

import java.util.List;

public record WorkspaceReferenceRecord(
        String relativePath,
        int version,
        String serverUrl,
        String projectId,
        String taskId,
        List<String> instructions
) {}
