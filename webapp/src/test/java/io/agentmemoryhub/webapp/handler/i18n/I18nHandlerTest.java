package io.agentmemoryhub.webapp.handler.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

public class I18nHandlerTest {
    @Test
    void combinesCommonAndModuleMessagesUsingTheSelectedLocale() {
        // Verifies common and module messages are loaded from the Spanish bundle for the resolved locale.
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(Locale.ENGLISH);
        var handler = new I18nHandler(messageSource);

        var messages = handler.getMessagesModules(Locale.forLanguageTag("es"), "ui", null);

        assertEquals("No tienes permiso para realizar esta operación.", messages.get("error.forbidden"));
        assertEquals("Iniciar sesión", messages.get("ui.login.heading"));
        assertFalse(messages.containsKey("api.route.notFound"));
    }
}
