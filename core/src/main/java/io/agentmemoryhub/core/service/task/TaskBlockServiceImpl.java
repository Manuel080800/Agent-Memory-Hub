package io.agentmemoryhub.core.service.task;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Task;
import io.agentmemoryhub.core.model.domain.TaskBlock;
import io.agentmemoryhub.core.model.record.TaskBlockInputRecord;
import io.agentmemoryhub.core.model.record.TaskBlockPageRecord;
import io.agentmemoryhub.core.model.record.TaskBlockRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.TaskBlockRepository;
import io.agentmemoryhub.core.repository.TaskRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskBlockServiceImpl implements TaskBlockService {
    private final TaskBlockRepository taskBlockRepository;
    private final TaskRepository taskRepository;
    private final ProfileRepository profileRepository;
    private final IdempotencyKeyService idempotencyKeyService;

    @Override
    @Transactional(readOnly = true)
    public TaskBlockPageRecord listTaskBlocksByTaskId(String taskId, int page, int pageSize) {
        findTaskById(taskId);
        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var taskBlockEntityList = taskBlockRepository.findByTaskIdOrderByCreatedAtAscIdAsc(taskId);
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, taskBlockEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, taskBlockEntityList.size());
        var taskBlockRecordList = taskBlockEntityList.subList(firstIndex, lastIndex).stream().map(this::toTaskBlockRecord).toList();
        return new TaskBlockPageRecord(taskBlockRecordList, normalizedPage, normalizedPageSize, taskBlockEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskBlockRecord getTaskBlockById(String blockId) {
        return toTaskBlockRecord(findTaskBlockById(blockId));
    }

    @Override
    @Transactional
    public TaskBlockRecord createTaskBlockByTaskIdAndInput(String taskId, TaskBlockInputRecord taskBlockInputRecord, String requestId) {
        validateTaskBlockInput(taskBlockInputRecord);
        var task = findTaskById(taskId);
        var profile = profileRepository.findById(task.getProfileId()).orElseThrow(() -> new HubException("INVALID_RELATION", "error.invalidRelation", 409, taskId));
        var taskBlock = new TaskBlock();
        taskBlock.setTaskId(taskId);
        taskBlock.setContent(taskBlockInputRecord.content());

        if (ValidateUtils.isValidString(requestId)) {
            var projectId = profile.getProjectId();
            var requestContent = taskBlockInputRecord.toString();
            var newBlockId = taskBlock.getId();
            var reservedBlockId = idempotencyKeyService.reserveResourceIdByRequest(projectId, "create_task_block", requestId, requestContent, newBlockId);

            if (!reservedBlockId.equals(taskBlock.getId())) {
                var existingTaskBlockOptional = taskBlockRepository.findById(reservedBlockId);
                var existingTaskBlock = existingTaskBlockOptional.orElseThrow(() -> new HubException("CONFLICT", "error.conflict", 409, reservedBlockId));
                return toTaskBlockRecord(existingTaskBlock);
            }
        }

        return toTaskBlockRecord(taskBlockRepository.save(taskBlock));
    }

    @Override
    @Transactional
    public TaskBlockRecord updateTaskBlockById(String blockId, TaskBlockInputRecord taskBlockInputRecord, Long expectedVersion) {
        validateTaskBlockInput(taskBlockInputRecord);
        var taskBlock = findTaskBlockById(blockId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion.longValue() != taskBlock.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, blockId);
        }

        taskBlock.setContent(taskBlockInputRecord.content());
        return toTaskBlockRecord(taskBlockRepository.save(taskBlock));
    }

    @Override
    @Transactional
    public void deleteTaskBlockById(String blockId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        taskBlockRepository.delete(findTaskBlockById(blockId));
    }

    private Task findTaskById(String taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, taskId));
    }

    private TaskBlock findTaskBlockById(String blockId) {
        return taskBlockRepository.findById(blockId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, blockId));
    }

    private void validateTaskBlockInput(TaskBlockInputRecord taskBlockInputRecord) {
        if (Objects.isNull(taskBlockInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "content");
        }

        if (ValidateUtils.isInvalidString(taskBlockInputRecord.content())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "content");
        }
    }

    private TaskBlockRecord toTaskBlockRecord(TaskBlock taskBlock) {
        return new TaskBlockRecord(taskBlock.getId(), taskBlock.getTaskId(), taskBlock.getContent(), taskBlock.getCreatedAt(), taskBlock.getUpdatedAt(), taskBlock.getVersion());
    }
}
