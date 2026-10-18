package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.PlatformRole;
import java.time.Instant;

public record UserRecord(
        String id,
        String fullName,
        String email,
        PlatformRole platformRole,
        boolean enabled,
        Instant createdAt,
        long version
) {}
