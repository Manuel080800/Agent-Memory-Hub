package io.agentmemoryhub.webapp.controller.task;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.TaskBlockInputRecord;
import io.agentmemoryhub.core.model.record.TaskBlockPageRecord;
import io.agentmemoryhub.core.model.record.TaskBlockRecord;
import io.agentmemoryhub.core.model.record.TaskRecord;
import io.agentmemoryhub.core.service.profile.ProfileService;
import io.agentmemoryhub.core.service.task.TaskBlockService;
import io.agentmemoryhub.core.service.task.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/profiles/{profileId}/tasks/{taskId}/blocks")
public class TaskBlockController {
    private final ProfileService profileService;
    private final TaskService taskService;
    private final TaskBlockService taskBlockService;

    @GetMapping
    public ResponseEntity<TaskBlockPageRecord> listTaskBlocks(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int pageSize) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return ResponseEntity.ok(taskBlockService.listTaskBlocksByTaskId(taskId, page, pageSize));
    }

    @PostMapping
    public ResponseEntity<TaskBlockRecord> createTaskBlock(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestBody TaskBlockInputRecord taskBlockInputRecord, @RequestHeader(name = "Idempotency-Key", required = false) String requestId) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return ResponseEntity.ok(taskBlockService.createTaskBlockByTaskIdAndInput(taskId, taskBlockInputRecord, requestId));
    }

    @PutMapping("/{blockId}")
    public ResponseEntity<TaskBlockRecord> updateTaskBlock(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String blockId, @RequestBody TaskBlockInputRecord taskBlockInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        validateTaskBlockRelation(projectId, profileId, taskId, blockId);
        return ResponseEntity.ok(taskBlockService.updateTaskBlockById(blockId, taskBlockInputRecord, expectedVersion));
    }

    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> deleteTaskBlock(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String blockId) {
        validateTaskBlockRelation(projectId, profileId, taskId, blockId);
        taskBlockService.deleteTaskBlockById(blockId, true);
        return ResponseEntity.noContent().build();
    }

    private TaskRecord findTaskByProjectIdAndProfileIdAndTaskId(String projectId, String profileId, String taskId) {
        var profileRecord = profileService.getProfileByIdAndProjectId(profileId, projectId);

        if (!projectId.equals(profileRecord.projectId())) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, profileId);
        }

        var taskRecord = taskService.getTaskById(taskId);

        if (!profileId.equals(taskRecord.profileId())) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, taskId);
        }

        return taskRecord;
    }

    private void validateTaskBlockRelation(String projectId, String profileId, String taskId, String blockId) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        var taskBlockRecord = taskBlockService.getTaskBlockById(blockId);

        if (!taskId.equals(taskBlockRecord.taskId())) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, blockId);
        }
    }
}
