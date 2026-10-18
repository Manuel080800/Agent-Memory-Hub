package io.agentmemoryhub.core.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidateUtilsTest {

    @Test
    void validatesStrings() {
        // Verifies blank strings are rejected and nonblank strings are accepted.
        assertTrue(ValidateUtils.isValidString("project-1"));
        assertFalse(ValidateUtils.isValidString("  "));
        assertTrue(ValidateUtils.isInvalidString(null));
    }
}
