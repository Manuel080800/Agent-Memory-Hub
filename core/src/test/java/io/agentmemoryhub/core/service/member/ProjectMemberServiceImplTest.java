package io.agentmemoryhub.core.service.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.common.ProjectRole;
import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.ProjectMemberInputRecord;
import io.agentmemoryhub.core.model.record.ProjectMemberRecord;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceImplTest {
    @Mock
    private ProjectMemberDAO projectMemberDAO;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectMemberServiceImpl projectMemberService;

    @Test
    void addsAnExistingUserAsAnEditor() {
        // Verifies valid member input is written and the inserted membership is returned.
        when(projectRepository.existsById("project-1")).thenReturn(true);
        when(userRepository.existsById("user-2")).thenReturn(true);
        when(projectMemberDAO.findProjectMembersByProjectId("project-1")).thenReturn(List.of(new ProjectMemberRecord("project-1", "user-2", ProjectRole.EDITOR, "Member", "member@example.com", null)));

        var memberRecord = projectMemberService.addProjectMemberByProjectIdAndInput("project-1", new ProjectMemberInputRecord("user-2", ProjectRole.EDITOR));

        verify(projectMemberDAO).addProjectMember("project-1", "user-2", ProjectRole.EDITOR);
        assertEquals("user-2", memberRecord.userId());
        assertEquals(ProjectRole.EDITOR, memberRecord.projectRole());
    }

    @Test
    void forbidsAssigningOwnerRoleAndRequiresConfirmationBeforeRemoval() {
        // Verifies ownership stays with the project owner and destructive removal requires confirmation.
        when(projectRepository.existsById("project-1")).thenReturn(true);
        var ownerRoleException = assertThrows(HubException.class, () -> projectMemberService.addProjectMemberByProjectIdAndInput("project-1", new ProjectMemberInputRecord("user-2", ProjectRole.OWNER)));
        var removalException = assertThrows(HubException.class, () -> projectMemberService.removeProjectMemberByProjectIdAndUserId("project-1", "user-2", false));

        assertEquals("OPERATION_NOT_ALLOWED", ownerRoleException.getCode());
        assertEquals("OPERATION_NOT_ALLOWED", removalException.getCode());
        verify(projectMemberDAO, never()).removeProjectMemberByProjectIdAndUserId("project-1", "user-2");
    }

    @Test
    void rejectsNewMembersWithoutAUserIdBeforeCheckingTheUser() {
        // Verifies missing member identity returns a validation error without reaching the repository.
        when(projectRepository.existsById("project-1")).thenReturn(true);

        var exception = assertThrows(HubException.class, () -> projectMemberService.addProjectMemberByProjectIdAndInput("project-1", new ProjectMemberInputRecord(null, ProjectRole.EDITOR)));

        assertEquals("INVALID_ARGUMENT", exception.getCode());
        verify(userRepository, never()).existsById(null);
        verify(projectMemberDAO, never()).addProjectMember("project-1", null, ProjectRole.EDITOR);
    }
}
