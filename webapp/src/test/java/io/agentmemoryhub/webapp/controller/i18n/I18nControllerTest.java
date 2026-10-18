package io.agentmemoryhub.webapp.controller.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.webapp.handler.i18n.I18nHandler;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

class I18nControllerTest {
    @Test
    void returnsLocalizedMessagesForTheRequestedModule() throws Exception {
        // Verifies the HTTP endpoint returns common and module translations in the request locale.
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(Locale.ENGLISH);
        var localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.forLanguageTag("es")));
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        var mockMvc = MockMvcBuilders.standaloneSetup(new I18nController(new I18nHandler(messageSource))).setLocaleResolver(localeResolver).build();

        var response = mockMvc.perform(get("/i18n/ui").header("Accept-Language", "es")).andReturn().getResponse();
        var responseJson = new ObjectMapper().readTree(response.getContentAsString());

        assertEquals(200, response.getStatus());
        assertEquals("No tienes permiso para realizar esta operación.", responseJson.path("error.forbidden").asText());
        assertEquals("Iniciar sesión", responseJson.path("ui.login.heading").asText());
    }
}
