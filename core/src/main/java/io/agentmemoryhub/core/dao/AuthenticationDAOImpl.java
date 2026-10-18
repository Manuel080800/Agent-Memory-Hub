package io.agentmemoryhub.core.dao;

import io.agentmemoryhub.core.model.record.PasswordCredentialRecord;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthenticationDAOImpl implements AuthenticationDAO {
    private final NamedParameterJdbcTemplate jdbcNativeTemplate;

    @Override
    public Optional<PasswordCredentialRecord> findPasswordCredentialByEmail(String email) {
        var sqlCommand = "SELECT id, password_hash FROM users WHERE lower(email) = lower(:email)";
        var params = new MapSqlParameterSource().addValue("email", email);
        var credentialRecordList = jdbcNativeTemplate.query(sqlCommand, params, (resultSet, rowNumber) -> new PasswordCredentialRecord(resultSet.getString("id"), resultSet.getString("password_hash")));
        return credentialRecordList.stream().findFirst();
    }

    @Override
    public void createSessionByTokenHashAndUserIdAndExpiry(String tokenHash, String userId, Instant expiresAt) {
        var sqlCommand = "INSERT INTO auth_sessions (token_hash, user_id, expires_at) VALUES (:tokenHash, :userId, :expiresAt)";
        var params = buildParamsForSessionCreation(tokenHash, userId, expiresAt);
        jdbcNativeTemplate.update(sqlCommand, params);
    }

    @Override
    public Optional<String> findUserIdByTokenHash(String tokenHash) {
        var sqlCommand = "SELECT user_id FROM auth_sessions WHERE token_hash = :tokenHash AND expires_at > now()";
        var params = new MapSqlParameterSource().addValue("tokenHash", tokenHash);
        var userIdList = jdbcNativeTemplate.query(sqlCommand, params, (resultSet, rowNumber) -> resultSet.getString("user_id"));
        return userIdList.stream().findFirst();
    }

    @Override
    public void deleteSessionByTokenHash(String tokenHash) {
        var sqlCommand = "DELETE FROM auth_sessions WHERE token_hash = :tokenHash";
        var params = new MapSqlParameterSource().addValue("tokenHash", tokenHash);
        jdbcNativeTemplate.update(sqlCommand, params);
    }


    private MapSqlParameterSource buildParamsForSessionCreation(String tokenHash, String userId, Instant expiresAt) {
        return new MapSqlParameterSource()
                .addValue("tokenHash", tokenHash)
                .addValue("userId", userId)
                .addValue("expiresAt", Timestamp.from(expiresAt));
    }
}
