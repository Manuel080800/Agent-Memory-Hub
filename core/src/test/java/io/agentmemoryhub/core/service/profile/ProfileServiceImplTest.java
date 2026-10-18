package io.agentmemoryhub.core.service.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.domain.Project;
import io.agentmemoryhub.core.model.record.ProfileInputRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {
    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Test
    void createsProfilesForEnabledProjectsAndTrimsTheirNames() {
        // Verifies profile creation checks its parent project and stores normalized input.
        when(projectRepository.findById("project-1")).thenReturn(Optional.of(new Project()));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var profileRecord = profileService.createProfileByProjectIdAndInput("project-1", new ProfileInputRecord("  Default  ", "description", null));
        var profileCaptor = ArgumentCaptor.forClass(Profile.class);

        verify(profileRepository).save(profileCaptor.capture());
        assertEquals("Default", profileRecord.name());
        assertEquals("project-1", profileCaptor.getValue().getProjectId());
    }

    @Test
    void clearsTheProjectActiveProfileWhenDisablingThatProfile() {
        // Verifies disabling the active profile clears the project reference before saving.
        var profile = new Profile();
        profile.setProjectId("project-1");
        profile.setName("Default");
        var project = new Project();
        project.setActiveProfileId(profile.getId());
        when(profileRepository.findByIdAndProjectId(profile.getId(), "project-1")).thenReturn(Optional.of(profile));
        when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));
        when(profileRepository.save(profile)).thenReturn(profile);

        profileService.updateProfileByIdAndProjectId(profile.getId(), "project-1", new ProfileInputRecord(null, null, false), 0L);

        assertEquals(null, project.getActiveProfileId());
        verify(projectRepository).save(project);
    }

    @Test
    void rejectsProfileUpdatesWhenTheExpectedVersionIsStale() {
        // Verifies optimistic locking prevents overwriting a profile with a stale version.
        var profile = new Profile();
        when(profileRepository.findByIdAndProjectId(profile.getId(), "project-1")).thenReturn(Optional.of(profile));

        var exception = assertThrows(HubException.class, () -> profileService.updateProfileByIdAndProjectId(profile.getId(), "project-1", new ProfileInputRecord("Renamed", null, null), 2L));

        assertEquals("CONFLICT", exception.getCode());
    }
}
