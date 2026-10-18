package io.agentmemoryhub.core.model.record;

import java.util.List;

public record ProjectMemberPageRecord(
        List<ProjectMemberRecord> items,
        int page,
        int pageSize,
        long total
) {}
