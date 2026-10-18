package io.agentmemoryhub.api.interceptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.common.ProjectRole;
import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.auth.AuthenticationService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class ApiAuthorizationInterceptorTest {
    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private ProjectMemberDAO projectMemberDAO;

    @Test
    void rejectsMissingBearerTokensWithUnauthorizedResponse() throws Exception {
        // Verifies unauthenticated API calls are stopped before the controller handler runs.
        var interceptor = createInterceptor();
        var request = new MockHttpServletRequest("GET", "/api/v1/projects");
        var response = new MockHttpServletResponse();
        when(authenticationService.findUserByAccessToken(null)).thenReturn(Optional.empty());

        var accepted = interceptor.preHandle(request, response, new Object());

        assertFalse(accepted);
        org.junit.jupiter.api.Assertions.assertEquals(401, response.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
    }

    @Test
    void allowsEditorsToReadProjectsButRequiresOwnersForMembershipChanges() throws Exception {
        // Verifies project roles permit editor reads but deny owner-only membership mutations.
        var interceptor = createInterceptor();
        var userRecord = new UserRecord("user-1", "Member", "member@example.com", PlatformRole.USER, true, Instant.now(), 0L);
        when(authenticationService.findUserByAccessToken("token")).thenReturn(Optional.of(userRecord));
        when(projectMemberDAO.findProjectRoleByProjectIdAndUserId("project-1", "user-1")).thenReturn(Optional.of(ProjectRole.EDITOR));
        var readRequest = createRequest("GET", "/api/v1/projects/project-1/memories");
        var readResponse = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(readRequest, readResponse, new Object()));
        assertTrue(readRequest.getAttribute("authenticatedUser") == userRecord);

        var mutationRequest = createRequest("POST", "/api/v1/projects/project-1/members");
        var mutationResponse = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(mutationRequest, mutationResponse, new Object()));
        org.junit.jupiter.api.Assertions.assertEquals(403, mutationResponse.getStatus());
    }

    private ApiAuthorizationInterceptor createInterceptor() {
        return new ApiAuthorizationInterceptor(authenticationService, projectMemberDAO, new StaticMessageSource(), new ObjectMapper());
    }

    private MockHttpServletRequest createRequest(String method, String requestPath) {
        var request = new MockHttpServletRequest(method, requestPath);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        return request;
    }
}
