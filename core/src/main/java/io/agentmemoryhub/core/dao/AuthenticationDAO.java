package io.agentmemoryhub.core.dao;

import io.agentmemoryhub.core.model.record.PasswordCredentialRecord;
import java.time.Instant;
import java.util.Optional;

public interface AuthenticationDAO {
    Optional<PasswordCredentialRecord> findPasswordCredentialByEmail(String email);

    void createSessionByTokenHashAndUserIdAndExpiry(String tokenHash, String userId, Instant expiresAt);

    Optional<String> findUserIdByTokenHash(String tokenHash);

    void deleteSessionByTokenHash(String tokenHash);
}
