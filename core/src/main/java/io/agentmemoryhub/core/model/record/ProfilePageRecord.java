package io.agentmemoryhub.core.model.record;

import java.util.List;

public record ProfilePageRecord(
        List<ProfileRecord> items,
        int page,
        int pageSize,
        long total
) {}
