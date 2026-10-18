package io.agentmemoryhub.core.model.record;

import java.util.List;

public record UserPageRecord(
        List<UserRecord> items,
        int page,
        int pageSize,
        long total
) {}
