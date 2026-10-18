package io.agentmemoryhub.core.service.user;

import io.agentmemoryhub.core.model.record.UserInputRecord;
import io.agentmemoryhub.core.model.record.UserPageRecord;
import io.agentmemoryhub.core.model.record.UserRecord;

public interface UserService {
    UserPageRecord listUsers(int page, int pageSize);

    UserRecord getUserById(String userId);

    UserRecord createUserByInput(UserInputRecord userInputRecord);

    UserRecord updateUserById(String userId, UserInputRecord userInputRecord, Long expectedVersion);

    void deleteUserById(String userId, boolean confirm);
}
