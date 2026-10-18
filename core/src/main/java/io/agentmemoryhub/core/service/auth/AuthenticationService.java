package io.agentmemoryhub.core.service.auth;

import io.agentmemoryhub.core.model.record.LoginInputRecord;
import io.agentmemoryhub.core.model.record.LoginResultRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import java.util.Optional;

public interface AuthenticationService {
    UserRecord authenticateByCredentials(LoginInputRecord loginInputRecord);

    LoginResultRecord loginByCredentials(LoginInputRecord loginInputRecord);

    Optional<UserRecord> findUserByAccessToken(String accessToken);

    void logoutByAccessToken(String accessToken);
}
