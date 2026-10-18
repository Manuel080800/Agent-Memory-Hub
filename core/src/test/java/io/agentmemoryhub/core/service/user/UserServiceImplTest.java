package io.agentmemoryhub.core.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.User;
import io.agentmemoryhub.core.model.record.UserInputRecord;
import io.agentmemoryhub.core.repository.UserRepository;
import io.agentmemoryhub.core.service.auth.PasswordHashService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberDAO projectMemberDAO;

    @Mock
    private PasswordHashService passwordHashService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createsUsersWithNormalizedEmailAndHashedPasswords() {
        // Verifies user creation normalizes the email and delegates password hashing.
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordHashService.hashPassword("long enough password")).thenReturn("safe-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var userRecord = userService.createUserByInput(new UserInputRecord("  Test User  ", " User@Example.COM ", "long enough password", null, null));
        var userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());
        verify(passwordHashService).hashPassword("long enough password");
        assertEquals("Test User", userRecord.fullName());
        assertEquals("user@example.com", userCaptor.getValue().getEmail());
        assertEquals(PlatformRole.USER, userRecord.platformRole());
    }

    @Test
    void rejectsDeletingUsersWhoStillBelongToProjects() {
        // Verifies project memberships prevent user deletion before the user repository is touched.
        when(projectMemberDAO.findProjectIdsByUserId("user-1")).thenReturn(java.util.List.of("project-1"));

        var exception = assertThrows(HubException.class, () -> userService.deleteUserById("user-1", true));

        assertEquals("CONFLICT", exception.getCode());
    }
}
