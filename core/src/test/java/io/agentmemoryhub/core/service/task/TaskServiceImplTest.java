package io.agentmemoryhub.core.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.common.ConnectionType;
import io.agentmemoryhub.core.common.TaskCreationMode;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.domain.Project;
import io.agentmemoryhub.core.model.domain.ProjectSettings;
import io.agentmemoryhub.core.model.domain.Task;
import io.agentmemoryhub.core.model.record.TaskInputRecord;
import io.agentmemoryhub.core.repository.McpConnectionRepository;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.ProjectSettingsRepository;
import io.agentmemoryhub.core.repository.TaskBlockRepository;
import io.agentmemoryhub.core.repository.TaskRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TaskServiceImplTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskBlockRepository taskBlockRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectSettingsRepository projectSettingsRepository;

    @Mock
    private McpConnectionRepository mcpConnectionRepository;

    @Mock
    private IdempotencyKeyService idempotencyKeyService;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void createsTasksOnlyForAnEnabledActiveProfile() {
        // Verifies task creation checks active project state and trims required text fields.
        var profile = new Profile();
        profile.setProjectId("project-1");
        var project = new Project();
        project.setActiveProfileId(profile.getId());
        when(profileRepository.findByIdAndEnabledTrue(profile.getId())).thenReturn(Optional.of(profile));
        when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var taskRecord = taskService.createTaskByProfileIdAndInput(profile.getId(), new TaskInputRecord("  Implement tests  ", "  Add service checks  ", null), null);
        var taskCaptor = ArgumentCaptor.forClass(Task.class);

        org.mockito.Mockito.verify(taskRepository).save(taskCaptor.capture());
        assertEquals("Implement tests", taskRecord.title());
        assertEquals("Add service checks", taskCaptor.getValue().getDescription());
        assertEquals("", taskRecord.summary());
    }

    @Test
    void classifiesPromptsAccordingToProjectTaskCreationMode() {
        // Verifies automatic task creation is suggested for complex prompts but not direct questions.
        var project = new Project();
        project.setActiveProfileId("profile-1");
        var settings = new ProjectSettings();
        settings.setProjectId(project.getId());
        settings.setTaskCreationMode(TaskCreationMode.AUTOMATIC);
        var activeProfile = new Profile();
        activeProfile.setProjectId(project.getId());
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectSettingsRepository.findById(project.getId())).thenReturn(Optional.of(settings));
        when(profileRepository.findByIdAndProjectIdAndEnabledTrue(project.getActiveProfileId(), project.getId())).thenReturn(Optional.of(activeProfile));

        var complexAnalysis = taskService.analyzeTaskPromptByProjectIdAndTaskId(project.getId(), "Build integration tests and update the data access layer", null);
        var questionAnalysis = taskService.analyzeTaskPromptByProjectIdAndTaskId(project.getId(), "How does authentication work?", null);

        assertTrue(complexAnalysis.shouldCreateAutomatically());
        assertEquals("MODERATE", complexAnalysis.complexity());
        assertFalse(questionAnalysis.shouldCreateAutomatically());
        assertEquals("SIMPLE", questionAnalysis.complexity());
    }

    @Test
    void analyzesTaskPromptsWhenSelectedTaskRetrievalIsDisabled() {
        // Verifies the task creation policy remains available when only selected-task retrieval is disabled.
        var project = new Project();
        project.setActiveProfileId("profile-1");
        var settings = new ProjectSettings();
        settings.setProjectId(project.getId());
        settings.setTaskCreationMode(TaskCreationMode.SUGGEST);
        settings.setSelectedTaskRetrievalEnabled(false);
        var activeProfile = new Profile();
        activeProfile.setProjectId(project.getId());
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectSettingsRepository.findById(project.getId())).thenReturn(Optional.of(settings));
        when(profileRepository.findByIdAndProjectIdAndEnabledTrue(project.getActiveProfileId(), project.getId())).thenReturn(Optional.of(activeProfile));

        var taskAnalysisRecord = taskService.analyzeTaskPromptByProjectIdAndTaskId(project.getId(), "Build a new integration test and update the service", null);

        assertTrue(taskAnalysisRecord.shouldSuggest());
        assertFalse(taskAnalysisRecord.shouldCreateAutomatically());
    }

    @Test
    void reportsTaskContinuityForAnUnselectedTaskWithoutLoadingTaskData() {
        // Verifies an empty task selection returns a continuity state without repository lookups.
        var project = new Project();
        project.setActiveProfileId("profile-1");
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        var continuityRecord = taskService.inspectTaskContinuityByProjectIdAndTaskId(project.getId(), null);

        assertFalse(continuityRecord.valid());
        assertEquals("TASK_NOT_SELECTED", continuityRecord.incompatibilityReason());
    }

    @Test
    void rejectsTaskCreationWhenTheProjectHasNoMatchingActiveProfile() {
        // Verifies task creation refuses an enabled profile that is not the project's active profile.
        var profile = new Profile();
        profile.setProjectId("project-1");
        var project = new Project();
        project.setActiveProfileId("another-profile");
        when(profileRepository.findByIdAndEnabledTrue(profile.getId())).thenReturn(Optional.of(profile));
        when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));

        var exception = assertThrows(HubException.class, () -> taskService.createTaskByProfileIdAndInput(profile.getId(), new TaskInputRecord("Task", "Description", null), null));

        assertEquals("NO_ACTIVE_PROFILE", exception.getCode());
    }

    @Test
    void rejectsTaskUpdatesWithoutChanges() {
        // Verifies task updates require at least one supplied field.
        var exception = assertThrows(HubException.class, () -> taskService.updateTaskById("task-1", new TaskInputRecord(null, null, null), null));

        assertEquals("INVALID_ARGUMENT", exception.getCode());
    }

    @Test
    void keepsTaskContextWithinAZeroTokenBudget() {
        // Verifies a task with no remaining quota returns metadata without falsely reporting included text tokens.
        var project = new Project();
        var profile = new Profile();
        project.setActiveProfileId(profile.getId());
        profile.setProjectId(project.getId());
        var projectSettings = new ProjectSettings();
        projectSettings.setProjectId(project.getId());
        projectSettings.setContextBudgetTokens(100);
        projectSettings.setPermanentQuotaPercent(100);
        var task = new Task();
        task.setProfileId(profile.getId());
        task.setTitle("A very long task title that cannot fit in the available context budget");
        task.setDescription("A detailed task description");
        task.setSummary("A useful summary");
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectSettingsRepository.findById(project.getId())).thenReturn(Optional.of(projectSettings));
        when(mcpConnectionRepository.findByProjectIdAndConnectionType(project.getId(), ConnectionType.TASKS)).thenReturn(Optional.empty());
        when(profileRepository.findByIdAndProjectIdAndEnabledTrue(profile.getId(), project.getId())).thenReturn(Optional.of(profile));
        when(taskRepository.findByIdAndProfileId(task.getId(), profile.getId())).thenReturn(Optional.of(task));
        when(taskBlockRepository.findByTaskIdOrderByCreatedAtAscIdAsc(task.getId())).thenReturn(List.of());

        var taskContextRecord = taskService.getTaskContextByProjectIdAndTaskId(project.getId(), task.getId(), null);

        assertEquals(0, taskContextRecord.taskBudgetTokens());
        assertEquals(0, taskContextRecord.estimatedTokens());
        assertEquals(0, taskContextRecord.task().estimatedTokens());
        assertTrue(taskContextRecord.task().truncated());
        assertTrue(taskContextRecord.task().title().isEmpty());
    }
}
