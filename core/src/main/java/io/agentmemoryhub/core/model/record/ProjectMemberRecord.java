package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ProjectRole;
import java.time.Instant;

public record ProjectMemberRecord(
        String projectId,
        String userId,
        ProjectRole projectRole,
        String fullName,
        String email,
        Instant createdAt
) {}
