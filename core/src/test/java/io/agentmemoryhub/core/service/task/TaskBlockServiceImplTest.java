package io.agentmemoryhub.core.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.domain.Task;
import io.agentmemoryhub.core.model.domain.TaskBlock;
import io.agentmemoryhub.core.model.record.TaskBlockInputRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.TaskBlockRepository;
import io.agentmemoryhub.core.repository.TaskRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TaskBlockServiceImplTest {
    @Mock
    private TaskBlockRepository taskBlockRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private IdempotencyKeyService idempotencyKeyService;

    @InjectMocks
    private TaskBlockServiceImpl taskBlockService;

    @Test
    void createsTaskBlocksForTheTaskProfile() {
        // Verifies task block creation resolves the task profile and persists the submitted content.
        var task = new Task();
        task.setProfileId("profile-1");
        var profile = new Profile();
        profile.setProjectId("project-1");
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(profileRepository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(taskBlockRepository.save(any(TaskBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var blockRecord = taskBlockService.createTaskBlockByTaskIdAndInput(task.getId(), new TaskBlockInputRecord("Decision notes"), null);
        var blockCaptor = ArgumentCaptor.forClass(TaskBlock.class);

        verify(taskBlockRepository).save(blockCaptor.capture());
        assertEquals(task.getId(), blockRecord.taskId());
        assertEquals("Decision notes", blockRecord.content());
        assertEquals(task.getId(), blockCaptor.getValue().getTaskId());
    }

    @Test
    void rejectsUnconfirmedDeletesAndStaleBlockVersions() {
        // Verifies block deletion requires confirmation and updates enforce optimistic versions.
        var unconfirmedDelete = assertThrows(HubException.class, () -> taskBlockService.deleteTaskBlockById("block-1", false));
        var taskBlock = new TaskBlock();
        when(taskBlockRepository.findById(taskBlock.getId())).thenReturn(Optional.of(taskBlock));
        var staleUpdate = assertThrows(HubException.class, () -> taskBlockService.updateTaskBlockById(taskBlock.getId(), new TaskBlockInputRecord("updated"), 2L));

        assertEquals("OPERATION_NOT_ALLOWED", unconfirmedDelete.getCode());
        assertEquals("CONFLICT", staleUpdate.getCode());
        verify(taskBlockRepository, never()).save(any(TaskBlock.class));
    }
}
