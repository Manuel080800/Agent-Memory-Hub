package io.agentmemoryhub.core.service.auth;

import io.agentmemoryhub.core.dao.AuthenticationDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.User;
import io.agentmemoryhub.core.model.record.LoginInputRecord;
import io.agentmemoryhub.core.model.record.LoginResultRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.repository.UserRepository;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final int ACCESS_TOKEN_LENGTH_BYTES = 32;
    private static final int SESSION_LIFETIME_HOURS = 8;

    private final AuthenticationDAO authenticationDAO;
    private final PasswordHashService passwordHashService;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public UserRecord authenticateByCredentials(LoginInputRecord loginInputRecord) {
        return toUserRecord(findEnabledUserByCredentials(loginInputRecord));
    }

    @Override
    @Transactional
    public LoginResultRecord loginByCredentials(LoginInputRecord loginInputRecord) {
        var user = findEnabledUserByCredentials(loginInputRecord);
        var tokenBytes = new byte[ACCESS_TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(tokenBytes);
        var accessToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        var expiresAt = Instant.now().plus(SESSION_LIFETIME_HOURS, ChronoUnit.HOURS);
        authenticationDAO.createSessionByTokenHashAndUserIdAndExpiry(hashToken(accessToken), user.getId(), expiresAt);
        return new LoginResultRecord(accessToken, expiresAt, toUserRecord(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRecord> findUserByAccessToken(String accessToken) {
        if (ValidateUtils.isInvalidString(accessToken)) {
            return Optional.empty();
        }

        var userIdOptional = authenticationDAO.findUserIdByTokenHash(hashToken(accessToken));
        var enabledUserOptional = userIdOptional.flatMap(userRepository::findById).filter(User::isEnabled);
        return enabledUserOptional.map(this::toUserRecord);
    }

    @Override
    @Transactional
    public void logoutByAccessToken(String accessToken) {
        if (ValidateUtils.isValidString(accessToken)) {
            authenticationDAO.deleteSessionByTokenHash(hashToken(accessToken));
        }
    }

    private boolean hasInvalidLoginInput(LoginInputRecord loginInputRecord) {
        if (Objects.isNull(loginInputRecord)) {
            return true;
        }

        return ValidateUtils.isInvalidString(loginInputRecord.email()) || Objects.isNull(loginInputRecord.password());
    }

    private User findEnabledUserByCredentials(LoginInputRecord loginInputRecord) {
        if (hasInvalidLoginInput(loginInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "credentials");
        }

        var passwordCredentialOptional = authenticationDAO.findPasswordCredentialByEmail(loginInputRecord.email().strip());

        if (passwordCredentialOptional.isEmpty()) {
            throw new HubException("UNAUTHORIZED", "error.unauthorized", 401);
        }

        var passwordCredential = passwordCredentialOptional.get();
        var passwordMatches = passwordHashService.matchesPassword(loginInputRecord.password(), passwordCredential.passwordHash());

        if (!passwordMatches) {
            throw new HubException("UNAUTHORIZED", "error.unauthorized", 401);
        }

        return userRepository.findById(passwordCredential.userId()).filter(User::isEnabled).orElseThrow(() -> new HubException("UNAUTHORIZED", "error.unauthorized", 401));
    }

    private String hashToken(String accessToken) {
        try {
            var tokenHash = MessageDigest.getInstance("SHA-256").digest(accessToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(tokenHash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private UserRecord toUserRecord(User user) {
        return new UserRecord(user.getId(), user.getFullName(), user.getEmail(), user.getPlatformRole(), user.isEnabled(), user.getCreatedAt(), user.getVersion());
    }
}
