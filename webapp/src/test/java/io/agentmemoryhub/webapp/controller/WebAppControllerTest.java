package io.agentmemoryhub.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.common.ConnectionType;
import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalRecord;
import io.agentmemoryhub.core.model.record.McpConnectionPageRecord;
import io.agentmemoryhub.core.model.record.McpConnectionRecord;
import io.agentmemoryhub.core.model.record.ProfileInputRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.ProjectInputRecord;
import io.agentmemoryhub.core.model.record.ProjectSettingsInputRecord;
import io.agentmemoryhub.core.model.record.ProposalApprovalInputRecord;
import io.agentmemoryhub.core.model.record.TaskBlockRecord;
import io.agentmemoryhub.core.model.record.TaskRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.connection.McpConnectionService;
import io.agentmemoryhub.core.service.member.ProjectMemberService;
import io.agentmemoryhub.core.service.profile.ProfileService;
import io.agentmemoryhub.core.service.project.ProjectService;
import io.agentmemoryhub.core.service.proposal.MemoryProposalService;
import io.agentmemoryhub.core.service.settings.ProjectSettingsService;
import io.agentmemoryhub.core.service.skill.SkillService;
import io.agentmemoryhub.core.service.task.TaskBlockService;
import io.agentmemoryhub.core.service.task.TaskService;
import io.agentmemoryhub.core.service.user.UserService;
import io.agentmemoryhub.core.service.memory.MemoryService;
import io.agentmemoryhub.core.service.workflow.WorkflowService;
import io.agentmemoryhub.webapp.controller.profile.ProfileController;
import io.agentmemoryhub.webapp.controller.profile.ProfileResourceController;
import io.agentmemoryhub.webapp.controller.project.McpConnectionController;
import io.agentmemoryhub.webapp.controller.project.ProjectController;
import io.agentmemoryhub.webapp.controller.project.ProjectMemberController;
import io.agentmemoryhub.webapp.controller.project.ProjectSettingsController;
import io.agentmemoryhub.webapp.controller.proposal.MemoryProposalController;
import io.agentmemoryhub.webapp.controller.task.TaskBlockController;
import io.agentmemoryhub.webapp.controller.user.UserController;
import io.agentmemoryhub.webapp.handler.session.WebAppSessionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

@ExtendWith(MockitoExtension.class)
public class WebAppControllerTest {
    @Mock
    private ProjectService projectService;

    @Mock
    private ProfileService profileService;

    @Mock
    private McpConnectionService mcpConnectionService;

    @Mock
    private ProjectMemberService projectMemberService;

    @Mock
    private ProjectSettingsService projectSettingsService;

    @Mock
    private SkillService skillService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private MemoryService memoryService;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskBlockService taskBlockService;

    @Mock
    private MemoryProposalService memoryProposalService;

    @Mock
    private UserService userService;

    private final WebAppSessionHandler sessionHandler = new WebAppSessionHandler();
    private ProjectController projectController;
    private ProfileController profileController;
    private ProfileResourceController profileResourceController;
    private McpConnectionController mcpConnectionController;
    private ProjectMemberController projectMemberController;
    private ProjectSettingsController projectSettingsController;
    private MemoryProposalController memoryProposalController;
    private TaskBlockController taskBlockController;
    private UserController userController;

    @BeforeEach
    void setUp() {
        projectController = new ProjectController(projectService, sessionHandler);
        profileController = new ProfileController(profileService, projectService);
        profileResourceController = new ProfileResourceController(profileService, skillService, workflowService, memoryService, taskService, new ObjectMapper());
        mcpConnectionController = new McpConnectionController(mcpConnectionService);
        projectMemberController = new ProjectMemberController(projectMemberService);
        projectSettingsController = new ProjectSettingsController(projectSettingsService);
        memoryProposalController = new MemoryProposalController(profileService, taskService, memoryProposalService);
        taskBlockController = new TaskBlockController(profileService, taskService, taskBlockService);
        userController = new UserController(userService, sessionHandler);
    }

    @Test
    void createsProjectsUsingTheSessionOwnerAndPreventsSelfDeletion() {
        // Verifies project ownership comes from the session and users cannot delete themselves.
        var userRecord = new UserRecord("user-1", "User", "user@example.com", PlatformRole.ADMIN, true, Instant.now(), 0L);
        var session = new MockHttpSession();
        sessionHandler.storeUser(session, userRecord);

        projectController.createProject(session, new ProjectInputRecord("Project", "Description", true, "spoofed-user"));
        var inputCaptor = ArgumentCaptor.forClass(ProjectInputRecord.class);
        verify(projectService).createProjectByInput(inputCaptor.capture());
        assertEquals("user-1", inputCaptor.getValue().ownerUserId());

        var exception = assertThrows(HubException.class, () -> userController.deleteUser(session, "user-1", true));
        assertEquals("OPERATION_NOT_ALLOWED", exception.getCode());
        verify(userService, never()).deleteUserById("user-1", true);
    }

    @Test
    void validatesProfileOwnershipBeforeDispatchingResourceRequests() {
        // Verifies resource requests dispatch by type only after the profile is confirmed to belong to the project.
        when(profileService.getProfileByIdAndProjectId("profile-1", "project-1")).thenReturn(new ProfileRecord("profile-1", "project-1", "Default", null, true, null, null, 0L));

        profileResourceController.listResources("project-1", "profile-1", "skills", 1, 5);

        verify(skillService).listSkillsByProfileId("profile-1", 1, 5);
        when(profileService.getProfileByIdAndProjectId("profile-2", "project-1")).thenReturn(new ProfileRecord("profile-2", "different-project", "Other", null, true, null, null, 0L));
        var exception = assertThrows(HubException.class, () -> profileResourceController.listResources("project-1", "profile-2", "skills", 0, 5));
        assertEquals("INVALID_RELATION", exception.getCode());
    }

    @Test
    void forwardsProfileAndProjectMutationsToTheirServices() {
        // Verifies profile changes and project settings retain route identifiers and version preconditions.
        profileController.updateProfile("project-1", "profile-1", new ProfileInputRecord(null, "Description", null), 7L);
        projectSettingsController.updateProjectSettings("project-1", new ProjectSettingsInputRecord(null, null, true, null, null, null, null, null, null), 9L);
        projectMemberController.removeMember("project-1", "user-2", true);

        verify(profileService).updateProfileByIdAndProjectId("profile-1", "project-1", new ProfileInputRecord(null, "Description", null), 7L);
        verify(projectSettingsService).updateProjectSettingsByProjectId("project-1", new ProjectSettingsInputRecord(null, null, true, null, null, null, null, null, null), 9L);
        verify(projectMemberService).removeProjectMemberByProjectIdAndUserId("project-1", "user-2", true);
    }

    @Test
    void rejectsMcpConnectionMutationsOutsideTheCurrentProject() {
        // Verifies a connection id must appear in the routed project's connection list before mutation.
        var connectionRecord = new McpConnectionRecord("connection-2", "project-1", "Context", ConnectionType.CONTEXT, true, null, null, 0L);
        when(mcpConnectionService.listMcpConnectionsByProjectId("project-1")).thenReturn(new McpConnectionPageRecord(List.of(connectionRecord), 0, 1, 1));

        var exception = assertThrows(HubException.class, () -> mcpConnectionController.deleteConnection("project-1", "connection-1", true));

        assertEquals("INVALID_RELATION", exception.getCode());
        verify(mcpConnectionService, never()).deleteMcpConnectionById("connection-1", true);
    }

    @Test
    void approvesOnlyProposalsBelongingToTheRequestedTask() {
        // Verifies proposal approval checks profile, task, and proposal ownership before building memory input.
        when(profileService.getProfileByIdAndProjectId("profile-1", "project-1")).thenReturn(new ProfileRecord("profile-1", "project-1", "Default", null, true, null, null, 0L));
        when(taskService.getTaskById("task-1")).thenReturn(new TaskRecord("task-1", "profile-1", "Task", "Description", "", null, null, 0L));
        when(memoryProposalService.getMemoryProposalById("proposal-1")).thenReturn(new MemoryProposalRecord("proposal-1", "task-1", "content", "reason", List.of(), false, null, null, null, 0L));

        memoryProposalController.approveMemoryProposal("project-1", "profile-1", "task-1", "proposal-1", new ProposalApprovalInputRecord("Memory", null, null, List.of("tag")));

        var memoryInputCaptor = ArgumentCaptor.forClass(MemoryInputRecord.class);
        verify(memoryProposalService).approveMemoryProposalById(org.mockito.ArgumentMatchers.eq("proposal-1"), memoryInputCaptor.capture());
        assertEquals(Boolean.TRUE, memoryInputCaptor.getValue().enabled());
        assertEquals(List.of("tag"), memoryInputCaptor.getValue().tags());
    }

    @Test
    void rejectsTaskBlockMutationsWhenTheBlockBelongsToAnotherTask() {
        // Verifies task blocks cannot be modified through a different task's route.
        when(profileService.getProfileByIdAndProjectId("profile-1", "project-1")).thenReturn(new ProfileRecord("profile-1", "project-1", "Default", null, true, null, null, 0L));
        when(taskService.getTaskById("task-1")).thenReturn(new TaskRecord("task-1", "profile-1", "Task", "Description", "", null, null, 0L));
        when(taskBlockService.getTaskBlockById("block-1")).thenReturn(new TaskBlockRecord("block-1", "task-2", "Content", null, null, 0L));

        var exception = assertThrows(HubException.class, () -> taskBlockController.deleteTaskBlock("project-1", "profile-1", "task-1", "block-1", true));

        assertEquals("INVALID_RELATION", exception.getCode());
        verify(taskBlockService, never()).deleteTaskBlockById("block-1", true);
    }
}
