package io.agentmemoryhub.webapp.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class WebAppLocaleAdviceTest {
    @Test
    void addsTheResolvedLanguageCodeToTheViewModel() {
        // Verifies templates receive the locale language selected for the current request.
        var model = new ExtendedModelMap();

        new WebAppLocaleAdvice().addLanguageAttribute(model, Locale.forLanguageTag("es-MX"));

        assertEquals("es", model.getAttribute("language"));
    }
}
