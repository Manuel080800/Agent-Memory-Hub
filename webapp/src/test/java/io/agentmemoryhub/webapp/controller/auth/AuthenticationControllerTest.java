package io.agentmemoryhub.webapp.controller.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.model.record.LoginInputRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.auth.AuthenticationService;
import io.agentmemoryhub.webapp.handler.session.WebAppSessionHandler;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

public class AuthenticationControllerTest {
    @Test
    void logsInOnlyWithAValidCsrfTokenAndStoresTheAuthenticatedUser() {
        // Verifies valid CSRF login authenticates and stores the user in the existing session.
        var authenticationService = Mockito.mock(AuthenticationService.class);
        var sessionHandler = new WebAppSessionHandler();
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("ui.login.failed", Locale.ENGLISH, "Login failed");
        var controller = new AuthenticationController(authenticationService, sessionHandler, messageSource);
        var request = new MockHttpServletRequest();
        var session = new MockHttpSession();
        var userRecord = new UserRecord("user-1", "Test User", "user@example.com", PlatformRole.USER, true, Instant.now(), 0L);
        var csrfToken = sessionHandler.getOrCreateCsrfToken(session);
        request.setSession(session);
        when(authenticationService.authenticateByCredentials(new LoginInputRecord("user@example.com", "long password"))).thenReturn(userRecord);

        var viewName = controller.loginByEmailAndPassword("user@example.com", "long password", csrfToken, request, session, new ExtendedModelMap(), Locale.ENGLISH);

        assertEquals("redirect:/dashboard", viewName);
        assertEquals(userRecord, sessionHandler.getUser(session));
    }

    @Test
    void showsLocalizedSessionExpiryWhenCsrfValidationFails() {
        // Verifies invalid CSRF submissions show the localized expiry message without authenticating.
        var authenticationService = Mockito.mock(AuthenticationService.class);
        var sessionHandler = new WebAppSessionHandler();
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("ui.login.sessionExpired", Locale.ENGLISH, "Session expired");
        var controller = new AuthenticationController(authenticationService, sessionHandler, messageSource);
        var model = new ExtendedModelMap();
        var viewName = controller.loginByEmailAndPassword("user@example.com", "password", "wrong-token", new MockHttpServletRequest(), new MockHttpSession(), model, Locale.ENGLISH);

        assertEquals("login", viewName);
        assertEquals("Session expired", model.getAttribute("errorMessage"));
        org.mockito.Mockito.verifyNoInteractions(authenticationService);
    }
}
