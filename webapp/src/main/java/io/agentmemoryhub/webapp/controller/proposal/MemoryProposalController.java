package io.agentmemoryhub.webapp.controller.proposal;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalPageRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalRecord;
import io.agentmemoryhub.core.model.record.ProposalApprovalInputRecord;
import io.agentmemoryhub.core.model.record.TaskRecord;
import io.agentmemoryhub.core.service.profile.ProfileService;
import io.agentmemoryhub.core.service.proposal.MemoryProposalService;
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
import java.util.Objects;

@Controller
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/profiles/{profileId}/tasks/{taskId}/proposals")
public class MemoryProposalController {
    private final ProfileService profileService;
    private final TaskService taskService;
    private final MemoryProposalService memoryProposalService;

    @GetMapping
    public ResponseEntity<MemoryProposalPageRecord> listTaskProposals(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int pageSize) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return ResponseEntity.ok(memoryProposalService.listMemoryProposalsByTaskId(taskId, page, pageSize));
    }

    @PostMapping
    public ResponseEntity<MemoryProposalRecord> createProposal(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @RequestBody MemoryProposalInputRecord memoryProposalInputRecord, @RequestHeader(name = "Idempotency-Key", required = false) String requestId) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        return ResponseEntity.ok(memoryProposalService.createMemoryProposalByTaskIdAndInput(taskId, memoryProposalInputRecord, requestId));
    }

    @PutMapping("/{proposalId}")
    public ResponseEntity<MemoryProposalRecord> updateProposal(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String proposalId, @RequestBody MemoryProposalInputRecord memoryProposalInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        validateProposalRelation(projectId, profileId, taskId, proposalId);
        return ResponseEntity.ok(memoryProposalService.updateMemoryProposalById(proposalId, memoryProposalInputRecord, expectedVersion));
    }

    @DeleteMapping("/{proposalId}")
    public ResponseEntity<Void> deleteProposal(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String proposalId) {
        validateProposalRelation(projectId, profileId, taskId, proposalId);
        memoryProposalService.deleteMemoryProposalById(proposalId, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{proposalId}/approve")
    public ResponseEntity<?> approveMemoryProposal(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String taskId, @PathVariable String proposalId, @RequestBody ProposalApprovalInputRecord proposalApprovalInputRecord) {
        validateProposalRelation(projectId, profileId, taskId, proposalId);

        if (Objects.isNull(proposalApprovalInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "title");
        }

        var memoryInputRecord = new MemoryInputRecord(proposalApprovalInputRecord.title(), proposalApprovalInputRecord.content(), proposalApprovalInputRecord.tags(), proposalApprovalInputRecord.applicationMode(), true);
        return ResponseEntity.ok(memoryProposalService.approveMemoryProposalById(proposalId, memoryInputRecord));
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

    private MemoryProposalRecord validateProposalRelation(String projectId, String profileId, String taskId, String proposalId) {
        findTaskByProjectIdAndProfileIdAndTaskId(projectId, profileId, taskId);
        var memoryProposalRecord = memoryProposalService.getMemoryProposalById(proposalId);

        if (!taskId.equals(memoryProposalRecord.taskId())) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, proposalId);
        }

        return memoryProposalRecord;
    }
}
