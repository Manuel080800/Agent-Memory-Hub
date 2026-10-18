package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ApplicationMode;
import java.util.List;

public record ProposalApprovalInputRecord(
        String title,
        ApplicationMode applicationMode,
        String content,
        List<String> tags
) {}
