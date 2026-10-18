package io.agentmemoryhub.core.service.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.common.ApplicationMode;
import io.agentmemoryhub.core.converter.TagsJsonDataConverter;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.domain.Skill;
import io.agentmemoryhub.core.model.record.SkillInputRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.SkillRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {
    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private IdempotencyKeyService idempotencyKeyService;

    private SkillServiceImpl skillService;

    @BeforeEach
    void setUp() {
        skillService = new SkillServiceImpl(skillRepository, profileRepository, new TagsJsonDataConverter(new ObjectMapper()), idempotencyKeyService);
    }

    @Test
    void createsSkillsWithNormalizedTagsAndContextualModeByDefault() {
        // Verifies skill creation applies text, tag, and application-mode defaults.
        var profile = new Profile();
        when(profileRepository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var skillRecord = skillService.createSkillByProfileIdAndInput("profile-1", new SkillInputRecord("  Java  ", null, "Use Java.", List.of(" Unit ", "unit"), null, null), null);
        var skillCaptor = ArgumentCaptor.forClass(Skill.class);

        verify(skillRepository).save(skillCaptor.capture());
        assertEquals("Java", skillRecord.name());
        assertEquals(List.of("Unit"), skillRecord.tags());
        assertEquals(ApplicationMode.CONTEXTUAL, skillRecord.applicationMode());
        assertEquals("", skillCaptor.getValue().getDescription());
    }

    @Test
    void rejectsStaleSkillUpdates() {
        // Verifies a mismatched expected version prevents the skill from being persisted.
        var skill = new Skill();
        when(skillRepository.findByIdAndProfileId(skill.getId(), "profile-1")).thenReturn(Optional.of(skill));

        var exception = assertThrows(HubException.class, () -> skillService.updateSkillByIdAndProfileId(skill.getId(), "profile-1", new SkillInputRecord(null, null, "Updated instructions", null, null, null), 1L));

        assertEquals("CONFLICT", exception.getCode());
    }

    @Test
    void rejectsBlankAndEmptySkillUpdatesBeforeSaving() {
        // Verifies skill updates require meaningful changes and reject blank required text.
        var blankNameException = assertThrows(HubException.class, () -> skillService.updateSkillByIdAndProfileId("skill-1", "profile-1", new SkillInputRecord(" ", null, null, null, null, null), null));
        var emptyChangesException = assertThrows(HubException.class, () -> skillService.updateSkillByIdAndProfileId("skill-1", "profile-1", new SkillInputRecord(null, null, null, null, null, null), null));

        assertEquals("INVALID_ARGUMENT", blankNameException.getCode());
        assertEquals("INVALID_ARGUMENT", emptyChangesException.getCode());
        verify(skillRepository, never()).save(any(Skill.class));
    }
}
