package io.agentmemoryhub.core.service.auth;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashServiceImpl implements PasswordHashService {
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 310_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String hashPassword(String password) {
        if (Objects.isNull(password) || password.length() < 12) {
            throw new IllegalArgumentException("Passwords must contain at least 12 characters.");
        }

        var salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        var derivedKey = deriveKey(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS);
        var encodedSalt = Base64.getEncoder().encodeToString(salt);
        var encodedDerivedKey = Base64.getEncoder().encodeToString(derivedKey);
        return "pbkdf2-sha256$" + ITERATION_COUNT + "$" + encodedSalt + "$" + encodedDerivedKey;
    }

    @Override
    public boolean matchesPassword(String password, String passwordHash) {
        if (Objects.isNull(password) || Objects.isNull(passwordHash)) {
            return false;
        }

        var hashParts = passwordHash.split("\\$", -1);

        if (hashParts.length != 4 || !"pbkdf2-sha256".equals(hashParts[0])) {
            return false;
        }

        try {
            var iterationCount = Integer.parseInt(hashParts[1]);
            var salt = Base64.getDecoder().decode(hashParts[2]);
            var expectedKey = Base64.getDecoder().decode(hashParts[3]);
            var actualKey = deriveKey(password.toCharArray(), salt, iterationCount, expectedKey.length * 8);
            return java.security.MessageDigest.isEqual(expectedKey, actualKey);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] deriveKey(char[] password, byte[] salt, int iterationCount, int keyLengthBits) {
        var keySpec = new PBEKeySpec(password, salt, iterationCount, keyLengthBits);

        try {
            var secretKeyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            return secretKeyFactory.generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        } finally {
            keySpec.clearPassword();
            Arrays.fill(password, '\0');
        }
    }
}
