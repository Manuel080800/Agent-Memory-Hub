package io.agentmemoryhub.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class ApiMessageConfigurationTest {
    @Test
    void providesSpanishMessagesAndFallsBackToEnglishForOtherLocales() {
        // Verifies the API supports Spanish and defaults unsupported locales to English messages.
        var messageSource = new ApiMessageConfiguration().messageSource();

        var spanishMessage = messageSource.getMessage("error.projectInactive", null, Locale.forLanguageTag("es"));
        var frenchMessage = messageSource.getMessage("error.projectInactive", null, Locale.FRENCH);
        var englishMessage = messageSource.getMessage("error.projectInactive", null, Locale.ENGLISH);

        assertEquals("El proyecto está inactivo.", spanishMessage);
        assertEquals(englishMessage, frenchMessage);
    }
}
