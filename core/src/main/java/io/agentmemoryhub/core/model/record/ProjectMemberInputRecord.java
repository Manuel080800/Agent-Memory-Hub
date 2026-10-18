package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ProjectRole;

public record ProjectMemberInputRecord(
        String userId,
        ProjectRole projectRole
) {}
