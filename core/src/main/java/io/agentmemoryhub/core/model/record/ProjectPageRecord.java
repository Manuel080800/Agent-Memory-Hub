package io.agentmemoryhub.core.model.record;

import java.util.List;

public record ProjectPageRecord(
        List<ProjectRecord> items,
        int page,
        int pageSize,
        long total
) {}
