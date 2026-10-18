package io.agentmemoryhub.core.service.auth;

public interface PasswordHashService {
    String hashPassword(String password);

    boolean matchesPassword(String password, String passwordHash);
}
