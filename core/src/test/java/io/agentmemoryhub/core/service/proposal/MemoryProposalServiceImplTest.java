package io.agentmemoryhub.core.service.proposal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.common.ApplicationMode;
import io.agentmemoryhub.core.converter.TagsJsonDataConverter;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Memory;
import io.agentmemoryhub.core.model.domain.MemoryProposal;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.domain.Task;
import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.repository.MemoryProposalRepository;
import io.agentmemoryhub.core.repository.MemoryRepository;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.TaskRepository;
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
public class MemoryProposalServiceImplTest {
    @Mock
    private MemoryProposalRepository memoryProposalRepository;

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private IdempotencyKeyService idempotencyKeyService;

    private MemoryProposalServiceImpl memoryProposalService;

    @BeforeEach
    void setUp() {
        var tagsJsonDataConverter = new TagsJsonDataConverter(new ObjectMapper());
        memoryProposalService = new MemoryProposalServiceImpl(memoryProposalRepository, memoryRepository, taskRepository, profileRepository, projectRepository, tagsJsonDataConverter, idempotencyKeyService);
    }

    @Test
    void approvalCreatesMemoryFromProposalDefaultsAndMarksProposalProcessed() {
        // Verifies approval inherits proposed content and tags and stores the processed proposal result.
        var proposal = new MemoryProposal();
        proposal.setTaskId("task-1");
        proposal.setProposedContent("Remember API boundaries");
        proposal.setReason("Repeated guidance");
        proposal.setTagsJson("[\"architecture\"]");
        var task = new Task();
        task.setProfileId("profile-1");
        var profile = new Profile();
        when(memoryProposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task));
        when(profileRepository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memoryProposalRepository.save(proposal)).thenReturn(proposal);

        var memoryRecord = memoryProposalService.approveMemoryProposalById(proposal.getId(), new MemoryInputRecord("  Architecture rule  ", null, null, null, null));
        var memoryCaptor = ArgumentCaptor.forClass(Memory.class);

        verify(memoryRepository).save(memoryCaptor.capture());
        verify(memoryProposalRepository).save(proposal);
        assertEquals("Architecture rule", memoryRecord.title());
        assertEquals("Remember API boundaries", memoryRecord.content());
        assertEquals(List.of("architecture"), memoryRecord.tags());
        assertEquals(ApplicationMode.CONTEXTUAL, memoryRecord.applicationMode());
        assertTrue(proposal.isProcessed());
        assertEquals(profile.getId(), memoryCaptor.getValue().getProfileId());
    }

    @Test
    void rejectsBlankContentWhenApprovingAProposal() {
        // Verifies explicit blank content cannot replace the proposed content with an invalid memory.
        var proposal = new MemoryProposal();
        when(memoryProposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));

        var exception = assertThrows(HubException.class, () -> memoryProposalService.approveMemoryProposalById(proposal.getId(), new MemoryInputRecord("Title", "   ", null, null, null)));

        assertEquals("INVALID_ARGUMENT", exception.getCode());
    }
}
