package io.agentmemoryhub.core.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.dao.AuthenticationDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.User;
import io.agentmemoryhub.core.model.record.LoginInputRecord;
import io.agentmemoryhub.core.model.record.PasswordCredentialRecord;
import io.agentmemoryhub.core.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {
    @Mock
    private AuthenticationDAO authenticationDAO;

    @Mock
    private PasswordHashService passwordHashService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    void logsInWithVerifiedCredentialsAndStoresOnlyATokenHash() {
        // Verifies successful login creates an expiring session without persisting the raw bearer token.
        var user = createEnabledUser();
        when(authenticationDAO.findPasswordCredentialByEmail("user@example.com")).thenReturn(Optional.of(new PasswordCredentialRecord(user.getId(), "password-hash")));
        when(passwordHashService.matchesPassword("a long password", "password-hash")).thenReturn(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var loginResult = authenticationService.loginByCredentials(new LoginInputRecord(" user@example.com ", "a long password"));
        var tokenHashCaptor = ArgumentCaptor.forClass(String.class);

        verify(authenticationDAO).createSessionByTokenHashAndUserIdAndExpiry(tokenHashCaptor.capture(), org.mockito.ArgumentMatchers.eq(user.getId()), any(Instant.class));
        assertEquals(user.getId(), loginResult.user().id());
        assertTrue(loginResult.expiresAt().isAfter(Instant.now()));
        assertEquals(64, tokenHashCaptor.getValue().length());
        assertFalse(loginResult.accessToken().equals(tokenHashCaptor.getValue()));
    }

    @Test
    void rejectsInvalidCredentialsAndSkipsStorageForBlankTokens() {
        // Verifies failed authentication is unauthorized and empty token lookups avoid the DAO.
        when(authenticationDAO.findPasswordCredentialByEmail("user@example.com")).thenReturn(Optional.of(new PasswordCredentialRecord("user-1", "password-hash")));
        when(passwordHashService.matchesPassword("wrong password", "password-hash")).thenReturn(false);

        var exception = assertThrows(HubException.class, () -> authenticationService.authenticateByCredentials(new LoginInputRecord("user@example.com", "wrong password")));

        assertEquals("UNAUTHORIZED", exception.getCode());
        assertTrue(authenticationService.findUserByAccessToken(" ").isEmpty());
        verify(authenticationDAO, never()).findUserIdByTokenHash(anyString());
    }

    @Test
    void findsEnabledUsersByTokenAndDeletesSessionsOnLogout() {
        // Verifies access tokens resolve only enabled users and logout deletes the hashed token.
        var user = createEnabledUser();
        when(authenticationDAO.findUserIdByTokenHash(anyString())).thenReturn(Optional.of(user.getId()));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var userRecord = authenticationService.findUserByAccessToken("access-token").orElseThrow();
        authenticationService.logoutByAccessToken("access-token");

        assertEquals(user.getId(), userRecord.id());
        var tokenHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(authenticationDAO).deleteSessionByTokenHash(tokenHashCaptor.capture());
        assertEquals(64, tokenHashCaptor.getValue().length());
        assertFalse("access-token".equals(tokenHashCaptor.getValue()));
    }

    private User createEnabledUser() {
        var user = new User();
        user.setFullName("Test User");
        user.setEmail("user@example.com");
        user.setPlatformRole(PlatformRole.USER);
        user.setEnabled(true);
        return user;
    }
}
