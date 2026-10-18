package io.agentmemoryhub.core.service.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.converter.TagsJsonDataConverter;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.domain.Project;
import io.agentmemoryhub.core.model.domain.ProjectSettings;
import io.agentmemoryhub.core.model.domain.Workflow;
import io.agentmemoryhub.core.repository.MemoryRepository;
import io.agentmemoryhub.core.repository.McpConnectionRepository;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.ProjectSettingsRepository;
import io.agentmemoryhub.core.repository.SkillRepository;
import io.agentmemoryhub.core.repository.WorkflowRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContextServiceImplTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectSettingsRepository projectSettingsRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private McpConnectionRepository mcpConnectionRepository;

    private ContextServiceImpl contextService;

    @BeforeEach
    void setUp() {
        var tagsJsonDataConverter = new TagsJsonDataConverter(new ObjectMapper());
        contextService = new ContextServiceImpl(projectRepository, profileRepository, projectSettingsRepository, workflowRepository, skillRepository, memoryRepository, mcpConnectionRepository, tagsJsonDataConverter);
    }

    @Test
    void returnsOnlyTheActiveProfileOfAnEnabledProject() {
        // Verifies the active profile is scoped to its project and must remain enabled.
        var project = new Project();
        var profile = new io.agentmemoryhub.core.model.domain.Profile();
        profile.setProjectId(project.getId());
        project.setActiveProfileId(profile.getId());
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(profileRepository.findByIdAndProjectIdAndEnabledTrue(profile.getId(), project.getId())).thenReturn(Optional.of(profile));

        var profileRecord = contextService.getActiveProfileByProjectId(project.getId());

        assertEquals(project.getId(), profileRecord.projectId());
        assertEquals(profile.getId(), profileRecord.id());
    }

    @Test
    void resolvesContextResourcesWithoutAllowingUnknownTypes() {
        // Verifies resource lookup normalizes type names and rejects unsupported resource types.
        var project = new Project();
        project.setActiveProfileId("profile-1");
        var workflow = new Workflow();
        workflow.setProfileId("profile-1");
        workflow.setName("Review");
        workflow.setDescription("Review changes");
        workflow.setInstructions("Check tests and docs");
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectSettingsRepository.findById(project.getId())).thenReturn(Optional.empty());
        when(mcpConnectionRepository.findByProjectIdAndConnectionType(project.getId(), io.agentmemoryhub.core.common.ConnectionType.CONTEXT)).thenReturn(Optional.empty());
        when(workflowRepository.findByIdAndProfileIdAndEnabledTrue(workflow.getId(), "profile-1")).thenReturn(Optional.of(workflow));

        var resourceRecord = contextService.getContextResourceByProjectIdAndTypeAndResourceId(project.getId(), "workflow", workflow.getId());
        var invalidTypeException = assertThrows(HubException.class, () -> contextService.getContextResourceByProjectIdAndTypeAndResourceId(project.getId(), "task", "task-1"));

        assertEquals("WORKFLOW", resourceRecord.resourceType());
        assertEquals("Review", resourceRecord.name());
        assertEquals("INVALID_ARGUMENT", invalidTypeException.getCode());
    }

    @Test
    void returnsAbbreviatedContextWhenResourcesExceedTheBudget() {
        // Verifies an over-budget recovery returns marked excerpts and counts omissions instead of failing.
        var project = new Project();
        var profile = new Profile();
        profile.setProjectId(project.getId());
        project.setActiveProfileId(profile.getId());
        var projectSettings = new ProjectSettings();
        projectSettings.setProjectId(project.getId());
        projectSettings.setContextBudgetTokens(100);
        projectSettings.setPermanentQuotaPercent(100);
        var workflow = new Workflow();
        workflow.setProfileId(profile.getId());
        workflow.setName("Review");
        workflow.setDescription("A workflow description");
        workflow.setInstructions("instruction ".repeat(100));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(profileRepository.findByIdAndProjectIdAndEnabledTrue(profile.getId(), project.getId())).thenReturn(Optional.of(profile));
        when(projectSettingsRepository.findById(project.getId())).thenReturn(Optional.of(projectSettings));
        when(mcpConnectionRepository.findByProjectIdAndConnectionType(project.getId(), io.agentmemoryhub.core.common.ConnectionType.CONTEXT)).thenReturn(Optional.empty());
        when(workflowRepository.findByProfileIdAndEnabledTrueOrderByCreatedAtAscIdAsc(profile.getId())).thenReturn(List.of(workflow));
        when(skillRepository.findByProfileIdAndEnabledTrueOrderByCreatedAtAscIdAsc(profile.getId())).thenReturn(List.of());
        when(memoryRepository.findByProfileIdAndEnabledTrueOrderByCreatedAtAscIdAsc(profile.getId())).thenReturn(List.of());

        var projectContextRecord = contextService.getProjectContextByProjectIdAndPrompt(project.getId(), null);

        assertTrue(projectContextRecord.truncated());
        assertTrue(projectContextRecord.workflows().getFirst().truncated());
        assertEquals("get_context_resource", projectContextRecord.workflows().getFirst().fullReadTool());
        assertEquals(0, projectContextRecord.omittedCount());
        assertTrue(projectContextRecord.estimatedTokens() <= projectContextRecord.permanentBudgetTokens());
    }
}
