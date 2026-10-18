package io.agentmemoryhub.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.api.model.McpJsonRpcRequestRecord;
import io.agentmemoryhub.api.service.McpProtocolService;
import io.agentmemoryhub.core.common.ConnectionType;
import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.McpConnectionPageRecord;
import io.agentmemoryhub.core.model.record.McpConnectionRecord;
import io.agentmemoryhub.core.model.record.ProjectInputRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.auth.AuthenticationService;
import io.agentmemoryhub.core.service.connection.McpConnectionService;
import io.agentmemoryhub.core.service.context.ContextService;
import io.agentmemoryhub.core.service.member.ProjectMemberService;
import io.agentmemoryhub.core.service.memory.MemoryService;
import io.agentmemoryhub.core.service.profile.ProfileService;
import io.agentmemoryhub.core.service.project.ProjectService;
import io.agentmemoryhub.core.service.settings.ProjectSettingsService;
import io.agentmemoryhub.core.service.skill.SkillService;
import io.agentmemoryhub.core.service.task.TaskBlockService;
import io.agentmemoryhub.core.service.task.TaskService;
import io.agentmemoryhub.core.service.proposal.MemoryProposalService;
import io.agentmemoryhub.core.service.user.UserService;
import io.agentmemoryhub.core.service.workflow.WorkflowService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ApiControllerTest {
    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private ContextService contextService;

    @Mock
    private SkillService skillService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private MemoryService memoryService;

    @Mock
    private McpConnectionService mcpConnectionService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProfileService profileService;

    @Mock
    private ProjectMemberService projectMemberService;

    @Mock
    private ProjectSettingsService projectSettingsService;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskBlockService taskBlockService;

    @Mock
    private MemoryProposalService memoryProposalService;

    @Mock
    private UserService userService;

    @Mock
    private McpProtocolService mcpProtocolService;

    @InjectMocks
    private AuthController authController;

    @InjectMocks
    private ContextController contextController;

    @InjectMocks
    private KnowledgeController knowledgeController;

    @InjectMocks
    private McpConnectionController mcpConnectionController;

    @InjectMocks
    private McpTransportController mcpTransportController;

    @InjectMocks
    private ProjectController projectController;

    @InjectMocks
    private ProjectMemberController projectMemberController;

    @InjectMocks
    private ProjectSettingsController projectSettingsController;

    @InjectMocks
    private TaskController taskController;

    @InjectMocks
    private UserController userController;

    @Test
    void authControllerExtractsBearerTokensForSessionOperations() {
        // Verifies authentication endpoints pass only the bearer token to the core service.
        var userRecord = new UserRecord("user-1", "Test User", "user@example.com", PlatformRole.USER, true, Instant.now(), 0L);
        when(authenticationService.findUserByAccessToken("session-token")).thenReturn(Optional.of(userRecord));
        var request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer session-token");

        assertEquals(userRecord, authController.getCurrentUser(request));
        assertEquals(204, authController.logout(request).getStatusCode().value());
        verify(authenticationService).logoutByAccessToken("session-token");
    }

    @Test
    void projectCreationUsesTheAuthenticatedOwnerRatherThanRequestOwnership() {
        // Verifies project creation derives owner identity from the authenticated request attribute.
        var userRecord = new UserRecord("authenticated-user", "User", "user@example.com", PlatformRole.USER, true, Instant.now(), 0L);
        var request = new MockHttpServletRequest();
        request.setAttribute("authenticatedUser", userRecord);

        projectController.createProject(request, new ProjectInputRecord("Project", null, null, "attacker-user"));

        var projectInputCaptor = ArgumentCaptor.forClass(ProjectInputRecord.class);
        verify(projectService).createProjectByInput(projectInputCaptor.capture());
        assertEquals("authenticated-user", projectInputCaptor.getValue().ownerUserId());
    }

    @Test
    void taskEndpointsRejectTasksOutsideTheRequestedProfile() {
        // Verifies task access is scoped to the profile and cross-profile requests stop before mutation.
        when(profileService.getProfileByIdAndProjectId("profile-1", "project-1")).thenReturn(new ProfileRecord("profile-1", "project-1", "Profile", null, true, null, null, 0L));
        when(taskService.getTaskById("task-1")).thenReturn(new io.agentmemoryhub.core.model.record.TaskRecord("task-1", "other-profile", "Task", "Description", "", null, null, 0L));

        var exception = assertThrows(HubException.class, () -> taskController.deleteTask("project-1", "profile-1", "task-1", true));

        assertEquals("INVALID_RELATION", exception.getCode());
        verify(taskService, never()).deleteTaskById("task-1", true);
    }

    @Test
    void connectionEndpointsRejectConnectionsFromAnotherProject() {
        // Verifies connection mutations are limited to a connection listed under the route project.
        var connectionRecord = new McpConnectionRecord("connection-1", "project-1", "Tasks", ConnectionType.TASKS, true, null, null, 0L);
        when(mcpConnectionService.listMcpConnectionsByProjectId("project-1")).thenReturn(new McpConnectionPageRecord(List.of(connectionRecord), 0, 1, 1));

        var exception = assertThrows(HubException.class, () -> mcpConnectionController.deleteConnection("project-1", "connection-2", true));

        assertEquals("INVALID_RELATION", exception.getCode());
        verify(mcpConnectionService, never()).deleteMcpConnectionById("connection-2", true);
    }

    @Test
    void userControllerForbidsDeletingTheAuthenticatedUser() {
        // Verifies users cannot delete their own account through the administrator endpoint.
        var request = new MockHttpServletRequest();
        request.setAttribute("authenticatedUser", new UserRecord("user-1", "User", "user@example.com", PlatformRole.ADMIN, true, null, 0L));

        var exception = assertThrows(HubException.class, () -> userController.deleteUser(request, "user-1", true));

        assertEquals("OPERATION_NOT_ALLOWED", exception.getCode());
        verify(userService, never()).deleteUserById("user-1", true);
    }

    @Test
    void mcpTransportRejectsInsecureRequestsAndAcceptsTrustedNotifications() {
        // Verifies MCP transport requires a trusted HTTPS loopback host and accepts notifications without a body.
        var requestRecord = new McpJsonRpcRequestRecord("2.0", null, "notifications/initialized", null);
        var untrustedRequest = new MockHttpServletRequest();
        untrustedRequest.addHeader(HttpHeaders.HOST, "localhost:8443");
        var untrustedResponse = mcpTransportController.handleContextRequest(requestRecord, untrustedRequest, Locale.ENGLISH);

        var trustedRequest = new MockHttpServletRequest();
        trustedRequest.setSecure(true);
        trustedRequest.addHeader(HttpHeaders.HOST, "localhost:8443");
        when(mcpProtocolService.handleRequestByEndpointAndRequest(io.agentmemoryhub.api.common.McpEndpointType.CONTEXT, requestRecord, Locale.ENGLISH)).thenReturn(Optional.empty());
        var trustedResponse = mcpTransportController.handleContextRequest(requestRecord, trustedRequest, Locale.ENGLISH);

        assertEquals(403, untrustedResponse.getStatusCode().value());
        assertEquals(202, trustedResponse.getStatusCode().value());
        assertEquals("2025-03-26", trustedResponse.getHeaders().getFirst("MCP-Protocol-Version"));
    }

    @Test
    void contextAndSettingsEndpointsDelegateTheRequestedProject() {
        // Verifies context and settings handlers forward the route project id to core services.
        contextController.getActiveProfile("project-1");
        projectSettingsController.resetProjectSettings("project-2");
        projectMemberController.listMembers("project-3");

        verify(contextService).getActiveProfileByProjectId("project-1");
        verify(projectSettingsService).resetProjectSettingsByProjectId("project-2");
        verify(projectMemberService).listProjectMembersByProjectId("project-3");
    }

    @Test
    void knowledgeControllerForwardsIdempotencyKeysForCreates() {
        // Verifies resource creation checks its project profile and passes the idempotency header to core.
        when(profileService.getProfileByIdAndProjectId("profile-1", "project-1")).thenReturn(new ProfileRecord("profile-1", "project-1", "Profile", null, true, null, null, 0L));

        knowledgeController.createMemory("project-1", "profile-1", "request-1", new io.agentmemoryhub.core.model.record.MemoryInputRecord("Title", "Content", List.of(), null, true));

        verify(memoryService).createMemoryByProfileIdAndInput(org.mockito.ArgumentMatchers.eq("profile-1"), any(), org.mockito.ArgumentMatchers.eq("request-1"));
    }

    @Test
    void knowledgeControllerRejectsProfilesOutsideTheRequestedProject() {
        // Verifies project-scoped resource routes cannot use a profile belonging to another project.
        when(profileService.getProfileByIdAndProjectId("profile-2", "project-1")).thenThrow(new HubException("NOT_FOUND", "error.notFound", 404, "profile-2"));

        var exception = assertThrows(HubException.class, () -> knowledgeController.listMemories("project-1", "profile-2", 0, 20));

        assertEquals("NOT_FOUND", exception.getCode());
        verify(memoryService, never()).listMemoriesByProfileId("profile-2", 0, 20);
    }
}
