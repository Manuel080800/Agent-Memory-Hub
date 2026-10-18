package io.agentmemoryhub.core.model.record;

import java.time.Instant;

public record LoginResultRecord(
        String accessToken,
        Instant expiresAt,
        UserRecord user
) {}
