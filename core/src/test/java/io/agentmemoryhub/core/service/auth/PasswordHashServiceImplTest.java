package io.agentmemoryhub.core.service.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PasswordHashServiceImplTest {
    private final PasswordHashServiceImpl passwordHashService = new PasswordHashServiceImpl();

    @Test
    void hashesPasswordsWithRandomSaltAndVerifiesOnlyMatchingPasswords() {
        // Verifies each stored password hash is salted and validates only the original password.
        var passwordHash = passwordHashService.hashPassword("correct horse battery");
        var secondPasswordHash = passwordHashService.hashPassword("correct horse battery");

        assertTrue(passwordHash.startsWith("pbkdf2-sha256$310000$"));
        assertTrue(passwordHashService.matchesPassword("correct horse battery", passwordHash));
        assertFalse(passwordHashService.matchesPassword("different password", passwordHash));
        assertFalse(passwordHash.equals(secondPasswordHash));
    }

    @Test
    void rejectsShortPasswordsAndMalformedHashes() {
        // Verifies password length constraints and malformed stored hashes fail safely.
        assertThrows(IllegalArgumentException.class, () -> passwordHashService.hashPassword("short"));
        assertFalse(passwordHashService.matchesPassword(null, "invalid"));
        assertFalse(passwordHashService.matchesPassword("some password", "pbkdf2-sha256$invalid$salt$key"));
    }
}
