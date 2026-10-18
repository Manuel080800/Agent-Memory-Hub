package io.agentmemoryhub.core.service.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Project;
import io.agentmemoryhub.core.model.domain.ProjectSettings;
import io.agentmemoryhub.core.model.record.ProjectInputRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.ProjectSettingsRepository;
import io.agentmemoryhub.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceImplTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectSettingsRepository projectSettingsRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberDAO projectMemberDAO;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void createsProjectSettingsAndOwnerMembershipTogether() {
        // Verifies project creation saves default settings and assigns the requested owner.
        when(userRepository.existsById("user-1")).thenReturn(true);
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var projectRecord = projectService.createProjectByInput(new ProjectInputRecord("  Project  ", "description", null, "user-1"));
        var projectCaptor = ArgumentCaptor.forClass(Project.class);
        var settingsCaptor = ArgumentCaptor.forClass(ProjectSettings.class);

        verify(projectRepository).saveAndFlush(projectCaptor.capture());
        verify(projectSettingsRepository).save(settingsCaptor.capture());
        verify(projectMemberDAO).addOwnerMember(projectRecord.id(), "user-1");
        assertEquals("Project", projectRecord.name());
        assertEquals(projectRecord.id(), settingsCaptor.getValue().getProjectId());
        assertEquals("user-1", projectCaptor.getValue().getOwnerUserId());
    }

    @Test
    void rejectsTransferOfOwnershipThroughProjectUpdates() {
        // Verifies an existing project owner cannot be replaced through the general update operation.
        var project = new Project();
        project.setOwnerUserId("owner-1");
        when(projectRepository.findById(project.getId())).thenReturn(java.util.Optional.of(project));

        var exception = assertThrows(HubException.class, () -> projectService.updateProjectById(project.getId(), new ProjectInputRecord(null, "description", null, "owner-2"), null));

        assertEquals("OPERATION_NOT_ALLOWED", exception.getCode());
    }
}
