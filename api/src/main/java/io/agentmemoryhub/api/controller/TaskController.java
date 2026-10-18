package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalPageRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalRecord;
import io.agentmemoryhub.core.model.record.MemoryRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.ProposalApprovalInputRecord;
import io.agentmemoryhub.core.model.record.TaskBlockInputRecord;
import io.agentmemoryhub.core.model.record.TaskBlockPageRecord;
import io.agentmemoryhub.core.model.record.TaskBlockRecord;
import io.agentmemoryhub.core.model.record.TaskContextRecord;
import io.agentmemoryhub.core.model.record.TaskInputRecord;
import io.agentmemoryhub.core.model.record.TaskPageRecord;
import io.agentmemoryhub.core.model.record.TaskRecord;
import io.agentmemoryhub.core.service.profile.ProfileService;
import io.agentmemoryhub.core.service.proposal.MemoryProposalService;
import io.agentmemoryhub.core.service.task.TaskBlockService;
import io.agentmemoryhub.core.service.task.TaskService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/profiles/{profileId}/tasks")
public class TaskController {
    private final ProfileService profileService;
    private final TaskService taskService;
    private final TaskBlockService taskBlockService;
    private final MemoryProposalService memoryProposalService;

    @GetMapping
    public TaskPageRecord listTasks(@PathVariable String projectId, @PathVariable String profileId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        validateProfileBelongsToProject(projectId, profileId);
        return taskService.listTasksByProfileId(profileId, page, pageSize);
    }

    @PostMapping
    public TaskRecord createTask(@PathVariable String projectId, @PathVariable String profileId, @RequestHeader(name = "Idempotency-Key", required = false) String requestId, @RequestBody TaskInputRecord taskInputRecord) {
        validateProfileBelongsToProject(projectId, profileId);
        return taskService.createTaskByProfileIdAndInput(profileId, taskInputRecord, requestId);
    }

    @GetMapping("/{taskId}")
    public TaskRecord getTask(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId) {
        return findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
    }

    @PutMapping("/{taskId}")
    public TaskRecord updateTask(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestHeader(name = "If-Match", required = false) Long expectedVersion, @RequestBody TaskInputRecord taskInputRecord) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return taskService.updateTaskById(taskId, taskInputRecord, expectedVersion);
    }

    @DeleteMapping("/{taskId}")
    public void deleteTask(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestParam(defaultValue = "false") boolean confirm) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        taskService.deleteTaskById(taskId, confirm);
    }

    @GetMapping("/{taskId}/context")
    public TaskContextRecord getTaskContext(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestParam(required = false) String prompt) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return taskService.getTaskContextByProjectIdAndTaskId(projectId, taskId, prompt);
    }

    @GetMapping("/{taskId}/blocks")
    public TaskBlockPageRecord listTaskBlocks(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return taskBlockService.listTaskBlocksByTaskId(taskId, page, pageSize);
    }

    @PostMapping("/{taskId}/blocks")
    public TaskBlockRecord createTaskBlock(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestHeader(name = "Idempotency-Key", required = false) String requestId, @RequestBody TaskBlockInputRecord taskBlockInputRecord) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return taskBlockService.createTaskBlockByTaskIdAndInput(taskId, taskBlockInputRecord, requestId);
    }

    @PutMapping("/{taskId}/blocks/{blockId}")
    public TaskBlockRecord updateTaskBlock(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String blockId, @RequestHeader(name = "If-Match", required = false) Long expectedVersion, @RequestBody TaskBlockInputRecord taskBlockInputRecord) {
        validateTaskBlockRelation(projectId, profileId, taskId, blockId);
        return taskBlockService.updateTaskBlockById(blockId, taskBlockInputRecord, expectedVersion);
    }

    @DeleteMapping("/{taskId}/blocks/{blockId}")
    public void deleteTaskBlock(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String blockId, @RequestParam(defaultValue = "false") boolean confirm) {
        validateTaskBlockRelation(projectId, profileId, taskId, blockId);
        taskBlockService.deleteTaskBlockById(blockId, confirm);
    }

    @GetMapping("/{taskId}/proposals")
    public MemoryProposalPageRecord listProposals(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return memoryProposalService.listMemoryProposalsByTaskId(taskId, page, pageSize);
    }

    @PostMapping("/{taskId}/proposals")
    public MemoryProposalRecord createProposal(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestHeader(name = "Idempotency-Key", required = false) String requestId, @RequestBody MemoryProposalInputRecord memoryProposalInputRecord) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return memoryProposalService.createMemoryProposalByTaskIdAndInput(taskId, memoryProposalInputRecord, requestId);
    }

    @GetMapping("/{taskId}/proposals/{proposalId}")
    public MemoryProposalRecord getProposal(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String proposalId) {
        return validateProposalRelation(projectId, profileId, taskId, proposalId);
    }

    @PutMapping("/{taskId}/proposals/{proposalId}")
    public MemoryProposalRecord updateProposal(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String proposalId, @RequestHeader(name = "If-Match", required = false) Long expectedVersion, @RequestBody MemoryProposalInputRecord memoryProposalInputRecord) {
        validateProposalRelation(projectId, profileId, taskId, proposalId);
        return memoryProposalService.updateMemoryProposalById(proposalId, memoryProposalInputRecord, expectedVersion);
    }

    @DeleteMapping("/{taskId}/proposals/{proposalId}")
    public void deleteProposal(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String proposalId, @RequestParam(defaultValue = "false") boolean confirm) {
        validateProposalRelation(projectId, profileId, taskId, proposalId);
        memoryProposalService.deleteMemoryProposalById(proposalId, confirm);
    }

    @PostMapping("/{taskId}/proposals/{proposalId}/approve")
    public MemoryRecord approveProposal(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String proposalId, @RequestBody ProposalApprovalInputRecord proposalApprovalInputRecord) {
        validateProposalRelation(projectId, profileId, taskId, proposalId);
        var memoryInputRecord = new MemoryInputRecord(proposalApprovalInputRecord.title(), proposalApprovalInputRecord.content(), proposalApprovalInputRecord.tags(), proposalApprovalInputRecord.applicationMode(), true);
        return memoryProposalService.approveMemoryProposalById(proposalId, memoryInputRecord);
    }

    private TaskRecord findTaskByProjectIdAndProfileIdAndTaskId(String projectId, String profileId, String taskId) {
        validateProfileBelongsToProject(projectId, profileId);
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

    private MemoryProposalRecord validateProposalRelation(String projectId, String profileId, String taskId, String proposalId) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        var memoryProposalRecord = memoryProposalService.getMemoryProposalById(proposalId);

        if (!taskId.equals(memoryProposalRecord.taskId())) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, proposalId);
        }

        return memoryProposalRecord;
    }

    private void validateProfileBelongsToProject(String projectId, String profileId) {
        var profileRecord = profileService.getProfileByIdAndProjectId(profileId, projectId);

        if (!projectId.equals(profileRecord.projectId())) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, profileId);
        }
    }
}
