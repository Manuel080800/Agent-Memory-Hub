package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.PlatformRole;

public record UserInputRecord(
        String fullName,
        String email,
        String password,
        PlatformRole platformRole,
        Boolean enabled
) {}
