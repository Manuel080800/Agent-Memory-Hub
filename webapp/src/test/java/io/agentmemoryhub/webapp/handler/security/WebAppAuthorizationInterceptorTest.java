package io.agentmemoryhub.webapp.handler.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.common.ProjectRole;
import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.user.UserService;
import io.agentmemoryhub.webapp.handler.session.WebAppSessionHandler;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

@ExtendWith(MockitoExtension.class)
public class WebAppAuthorizationInterceptorTest {
    @Mock
    private ProjectMemberDAO projectMemberDAO;

    @Mock
    private UserService userService;

    private final WebAppSessionHandler sessionHandler = new WebAppSessionHandler();

    @Test
    void rejectsMissingUsersAndRequiresOwnerRoleForMembershipChanges() throws Exception {
        // Verifies protected pages reject missing sessions and editor accounts cannot mutate memberships.
        var interceptor = createInterceptor();
        var noSessionRequest = new MockHttpServletRequest("GET", "/projects/project-1");
        var noSessionResponse = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(noSessionRequest, noSessionResponse, new Object()));
        org.junit.jupiter.api.Assertions.assertEquals(401, noSessionResponse.getStatus());

        var userRecord = new UserRecord("user-1", "Editor", "editor@example.com", PlatformRole.USER, true, Instant.now(), 0L);
        when(userService.getUserById("user-1")).thenReturn(userRecord);
        when(projectMemberDAO.findProjectRoleByProjectIdAndUserId("project-1", "user-1")).thenReturn(Optional.of(ProjectRole.EDITOR));
        var membershipRequest = createAuthenticatedRequest("POST", "/projects/project-1/members", userRecord);
        var membershipResponse = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(membershipRequest, membershipResponse, new Object()));
        org.junit.jupiter.api.Assertions.assertEquals(403, membershipResponse.getStatus());
    }

    @Test
    void allowsEditorMutationsOnlyWhenTheCsrfTokenMatchesTheSession() throws Exception {
        // Verifies editor write access to ordinary project resources still requires a valid CSRF token.
        var userRecord = new UserRecord("user-1", "Editor", "editor@example.com", PlatformRole.USER, true, Instant.now(), 0L);
        when(userService.getUserById("user-1")).thenReturn(userRecord);
        when(projectMemberDAO.findProjectRoleByProjectIdAndUserId("project-1", "user-1")).thenReturn(Optional.of(ProjectRole.EDITOR));
        var request = createAuthenticatedRequest("POST", "/projects/project-1/memories", userRecord);
        var response = new MockHttpServletResponse();

        assertTrue(createInterceptor().preHandle(request, response, new Object()));

        var invalidRequest = new MockHttpServletRequest("POST", "/projects/project-1/memories");
        var invalidSession = new MockHttpSession();
        sessionHandler.storeUser(invalidSession, userRecord);
        sessionHandler.getOrCreateCsrfToken(invalidSession);
        invalidRequest.setSession(invalidSession);
        invalidRequest.addHeader("X-CSRF-Token", "wrong-token");
        var invalidResponse = new MockHttpServletResponse();
        assertFalse(createInterceptor().preHandle(invalidRequest, invalidResponse, new Object()));
        org.junit.jupiter.api.Assertions.assertEquals(403, invalidResponse.getStatus());
    }

    private WebAppAuthorizationInterceptor createInterceptor() {
        return new WebAppAuthorizationInterceptor(projectMemberDAO, sessionHandler, userService, new StaticMessageSource(), new ObjectMapper());
    }

    private MockHttpServletRequest createAuthenticatedRequest(String method, String requestPath, UserRecord userRecord) {
        var request = new MockHttpServletRequest(method, requestPath);
        var session = new MockHttpSession();
        sessionHandler.storeUser(session, userRecord);
        sessionHandler.getOrCreateCsrfToken(session);
        request.setSession(session);
        request.addHeader("X-CSRF-Token", sessionHandler.getOrCreateCsrfToken(session));
        return request;
    }
}
