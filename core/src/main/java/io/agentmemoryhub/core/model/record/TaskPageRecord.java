package io.agentmemoryhub.core.model.record;

import java.util.List;

public record TaskPageRecord(
        List<TaskRecord> items,
        int page,
        int pageSize,
        long total
) {}
