package io.agentmemoryhub.webapp.handler.session;

import io.agentmemoryhub.core.model.record.UserRecord;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WebAppSessionHandler {
    private static final String USER_SESSION_ATTRIBUTE = "agentMemoryHubUser";
    private static final String CSRF_TOKEN_SESSION_ATTRIBUTE = "agentMemoryHubCsrfToken";

    public String getOrCreateCsrfToken(HttpSession httpSession) {
        var csrfToken = httpSession.getAttribute(CSRF_TOKEN_SESSION_ATTRIBUTE);

        if (csrfToken instanceof String token) {
            return token;
        }

        var generatedCsrfToken = UUID.randomUUID().toString();
        httpSession.setAttribute(CSRF_TOKEN_SESSION_ATTRIBUTE, generatedCsrfToken);
        return generatedCsrfToken;
    }

    public boolean isValidCsrfToken(HttpSession httpSession, String requestCsrfToken) {
        var sessionCsrfToken = httpSession.getAttribute(CSRF_TOKEN_SESSION_ATTRIBUTE);
        return sessionCsrfToken instanceof String token && token.equals(requestCsrfToken);
    }

    public boolean hasUser(HttpSession httpSession) {
        return Objects.nonNull(getUser(httpSession));
    }

    public UserRecord getUser(HttpSession httpSession) {
        var user = httpSession.getAttribute(USER_SESSION_ATTRIBUTE);
        return user instanceof UserRecord userRecord ? userRecord : null;
    }

    public void storeUser(HttpSession httpSession, UserRecord userRecord) {
        httpSession.setAttribute(USER_SESSION_ATTRIBUTE, userRecord);
    }
}
