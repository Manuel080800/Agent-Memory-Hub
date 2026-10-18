package io.agentmemoryhub.core.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.domain.Workflow;
import io.agentmemoryhub.core.model.record.WorkflowInputRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.WorkflowRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceImplTest {
    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private IdempotencyKeyService idempotencyKeyService;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    @Test
    void createsWorkflowWithTrimmedNameAndDefaultDescription() {
        // Verifies workflow creation normalizes its name and uses an empty description when omitted.
        when(profileRepository.findById("profile-1")).thenReturn(Optional.of(new Profile()));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var workflowRecord = workflowService.createWorkflowByProfileIdAndInput("profile-1", new WorkflowInputRecord("  Review  ", null, "review instructions", null), null);
        var workflowCaptor = ArgumentCaptor.forClass(Workflow.class);

        verify(workflowRepository).save(workflowCaptor.capture());
        assertEquals("Review", workflowRecord.name());
        assertEquals("", workflowCaptor.getValue().getDescription());
    }

    @Test
    void rejectsCreationWithoutInstructionsAndDetectsStaleUpdates() {
        // Verifies required workflow fields and optimistic version checks reject invalid mutations.
        var invalidInputException = assertThrows(HubException.class, () -> workflowService.createWorkflowByProfileIdAndInput("profile-1", new WorkflowInputRecord("Review", null, " ", null), null));
        var workflow = new Workflow();
        when(workflowRepository.findByIdAndProfileId(workflow.getId(), "profile-1")).thenReturn(Optional.of(workflow));
        var staleUpdateException = assertThrows(HubException.class, () -> workflowService.updateWorkflowByIdAndProfileId(workflow.getId(), "profile-1", new WorkflowInputRecord(null, null, "updated", null), 4L));

        assertEquals("INVALID_ARGUMENT", invalidInputException.getCode());
        assertEquals("CONFLICT", staleUpdateException.getCode());
    }

    @Test
    void rejectsBlankAndEmptyWorkflowUpdatesBeforeSaving() {
        // Verifies workflow updates reject blank required text and empty change sets.
        var blankInstructionsException = assertThrows(HubException.class, () -> workflowService.updateWorkflowByIdAndProfileId("workflow-1", "profile-1", new WorkflowInputRecord(null, null, " ", null), null));
        var emptyChangesException = assertThrows(HubException.class, () -> workflowService.updateWorkflowByIdAndProfileId("workflow-1", "profile-1", new WorkflowInputRecord(null, null, null, null), null));

        assertEquals("INVALID_ARGUMENT", blankInstructionsException.getCode());
        assertEquals("INVALID_ARGUMENT", emptyChangesException.getCode());
        verify(workflowRepository, never()).save(any(Workflow.class));
    }
}
