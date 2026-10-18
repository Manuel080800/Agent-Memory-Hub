package io.agentmemoryhub.core.model.record;

import java.util.List;

public record MemoryPageRecord(
        List<MemoryRecord> items,
        int page,
        int pageSize,
        long total
) {}
