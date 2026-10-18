package io.agentmemoryhub.core.model.record;

public record PasswordCredentialRecord(
        String userId,
        String passwordHash
) {}
