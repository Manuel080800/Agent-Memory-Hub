package io.agentmemoryhub.core.utils;

import java.util.Objects;

public final class ValidateUtils {
    private ValidateUtils() {}

    public static boolean isValidString(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }

    public static boolean isInvalidString(String value) {
        return !isValidString(value);
    }
}
