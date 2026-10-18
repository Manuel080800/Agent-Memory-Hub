package io.agentmemoryhub.core.model.record;

import java.util.List;

public record TaskBlockPageRecord(
        List<TaskBlockRecord> items,
        int page,
        int pageSize,
        long total
) {}
