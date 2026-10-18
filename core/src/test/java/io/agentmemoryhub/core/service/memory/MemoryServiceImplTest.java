package io.agentmemoryhub.core.service.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.common.ApplicationMode;
import io.agentmemoryhub.core.converter.TagsJsonDataConverter;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Memory;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.repository.MemoryRepository;
import io.agentmemoryhub.core.repository.ProfileRepository;
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
class MemoryServiceImplTest {
    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private IdempotencyKeyService idempotencyKeyService;

    private MemoryServiceImpl memoryService;

    private final TagsJsonDataConverter tagsJsonDataConverter = new TagsJsonDataConverter(new ObjectMapper());

    @BeforeEach
    void setUp() {
        memoryService = new MemoryServiceImpl(memoryRepository, profileRepository, tagsJsonDataConverter, idempotencyKeyService);
    }

    @Test
    void createsMemoryWithNormalizedTagsAndDefaultMode() {
        // Verifies input normalization and defaults are applied before a new memory is saved.
        var profile = new Profile();
        profile.setProjectId("project-1");
        when(profileRepository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var memoryRecord = memoryService.createMemoryByProfileIdAndInput("profile-1", new MemoryInputRecord("  Design notes  ", "Remember the API boundary.", List.of(" Java ", "java", "API"), null, null), null);
        var memoryCaptor = ArgumentCaptor.forClass(Memory.class);

        verify(memoryRepository).save(memoryCaptor.capture());
        assertEquals("Design notes", memoryRecord.title());
        assertEquals(List.of("Java", "API"), memoryRecord.tags());
        assertEquals(ApplicationMode.CONTEXTUAL, memoryRecord.applicationMode());
        assertEquals("profile-1", memoryCaptor.getValue().getProfileId());
    }

    @Test
    void rejectsMemoryCreationWithoutARequiredTitle() {
        // Verifies blank required fields fail before profile or persistence access.
        var exception = assertThrows(HubException.class, () -> memoryService.createMemoryByProfileIdAndInput("profile-1", new MemoryInputRecord("  ", "content", List.of(), null, null), null));

        assertEquals("INVALID_ARGUMENT", exception.getCode());
        verify(profileRepository, never()).findById("profile-1");
        verify(memoryRepository, never()).save(any(Memory.class));
    }

    @Test
    void rejectsBlankAndEmptyMemoryUpdatesBeforeSaving() {
        // Verifies memory updates reject blank required text and empty change sets.
        var blankContentException = assertThrows(HubException.class, () -> memoryService.updateMemoryByIdAndProfileId("memory-1", "profile-1", new MemoryInputRecord(null, " ", null, null, null), null));
        var emptyChangesException = assertThrows(HubException.class, () -> memoryService.updateMemoryByIdAndProfileId("memory-1", "profile-1", new MemoryInputRecord(null, null, null, null, null), null));

        assertEquals("INVALID_ARGUMENT", blankContentException.getCode());
        assertEquals("INVALID_ARGUMENT", emptyChangesException.getCode());
        verify(memoryRepository, never()).save(any(Memory.class));
    }
}
