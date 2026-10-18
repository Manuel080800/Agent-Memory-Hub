package io.agentmemoryhub.core.model.record;

import java.util.List;

public record SkillPageRecord(
        List<SkillRecord> items,
        int page,
        int pageSize,
        long total
) {}
