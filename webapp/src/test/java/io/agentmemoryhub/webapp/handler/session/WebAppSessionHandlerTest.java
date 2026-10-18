package io.agentmemoryhub.webapp.handler.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.model.record.UserRecord;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

public class WebAppSessionHandlerTest {
    private final WebAppSessionHandler sessionHandler = new WebAppSessionHandler();

    @Test
    void storesUserAndReusesTheSessionCsrfToken() {
        // Verifies session user state and CSRF tokens persist across controller requests.
        var session = new MockHttpSession();
        var userRecord = new UserRecord("user-1", "Test User", "user@example.com", PlatformRole.USER, true, Instant.now(), 0L);
        var firstCsrfToken = sessionHandler.getOrCreateCsrfToken(session);

        sessionHandler.storeUser(session, userRecord);

        assertNotEquals("", firstCsrfToken);
        assertEquals(firstCsrfToken, sessionHandler.getOrCreateCsrfToken(session));
        assertTrue(sessionHandler.hasUser(session));
        assertEquals(userRecord, sessionHandler.getUser(session));
        assertTrue(sessionHandler.isValidCsrfToken(session, firstCsrfToken));
        assertFalse(sessionHandler.isValidCsrfToken(session, "different-token"));
    }
}
