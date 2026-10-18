package io.agentmemoryhub.core.model.record;

import java.util.List;

public record MemoryProposalPageRecord(
        List<MemoryProposalRecord> items,
        int page,
        int pageSize,
        long total
) {}
