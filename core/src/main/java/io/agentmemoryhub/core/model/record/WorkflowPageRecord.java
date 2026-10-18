package io.agentmemoryhub.core.model.record;

import java.util.List;

public record WorkflowPageRecord(
        List<WorkflowRecord> items,
        int page,
        int pageSize,
        long total
) {}
