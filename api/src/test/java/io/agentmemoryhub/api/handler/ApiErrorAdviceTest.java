package io.agentmemoryhub.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agentmemoryhub.core.exception.HubException;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.converter.HttpMessageNotReadableException;

class ApiErrorAdviceTest {
    @Test
    void mapsHubErrorsToLocalizedResponsesAndPreservesDetails() {
        // Verifies domain error status, message arguments, and detail entries are returned to API clients.
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("error.conflict", Locale.ENGLISH, "Conflict on {0}");
        var advice = new ApiErrorAdvice(messageSource);

        var response = advice.handleHubException(new HubException("CONFLICT", "error.conflict", 409, List.of("field"), null, "project"), Locale.ENGLISH);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict on project", response.getBody().message());
        assertEquals("field", response.getBody().details().getFirst().code());
    }

    @Test
    void mapsUnreadableJsonToValidationFailure() {
        // Verifies malformed request bodies receive the same localized validation response as bad arguments.
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("error.validation", Locale.ENGLISH, "Invalid request");
        var advice = new ApiErrorAdvice(messageSource);

        var response = advice.handleInvalidRequest(new HttpMessageNotReadableException("bad json", null), Locale.ENGLISH);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("VALIDATION", response.getBody().code());
        assertEquals("Invalid request", response.getBody().message());
    }
}
