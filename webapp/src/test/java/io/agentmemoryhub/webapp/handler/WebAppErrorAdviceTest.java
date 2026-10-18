package io.agentmemoryhub.webapp.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.exception.HubException;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

public class WebAppErrorAdviceTest {
    @Test
    void returnsLocalizedDomainErrorsWithTheirHttpStatusAndDetails() {
        // Verifies domain failures preserve their status, translated message, and detail list.
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("error.notFound", Locale.forLanguageTag("es"), "No encontrado: {0}");
        var advice = new WebAppErrorAdvice(messageSource, new ObjectMapper());

        var response = advice.handleHubException(new HubException("NOT_FOUND", "error.notFound", 404, List.of("field"), null, "profile-1"), Locale.forLanguageTag("es"));

        assertEquals(404, response.getStatusCode().value());
        assertEquals("No encontrado: profile-1", response.getBody().message());
        assertEquals("field", response.getBody().details().getFirst().code());
    }

    @Test
    void mapsInvalidArgumentsToJsonBadRequestBodies() {
        // Verifies argument binding failures become localized JSON responses with HTTP 400.
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("webapp.error.invalidRequest", Locale.ENGLISH, "Invalid request");
        var advice = new WebAppErrorAdvice(messageSource, new ObjectMapper());

        var response = advice.handleInvalidRequest(new IllegalArgumentException("bad parameter"), Locale.ENGLISH);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid request", response.getBody().get("message").asText());
    }
}
