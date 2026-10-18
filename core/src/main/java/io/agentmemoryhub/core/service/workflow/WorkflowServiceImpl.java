package io.agentmemoryhub.core.service.workflow;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Workflow;
import io.agentmemoryhub.core.model.record.WorkflowInputRecord;
import io.agentmemoryhub.core.model.record.WorkflowPageRecord;
import io.agentmemoryhub.core.model.record.WorkflowRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.WorkflowRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {
    private final WorkflowRepository workflowRepository;
    private final ProfileRepository profileRepository;
    private final IdempotencyKeyService idempotencyKeyService;

    @Override
    @Transactional(readOnly = true)
    public WorkflowPageRecord listWorkflowsByProfileId(String profileId, int page, int pageSize) {
        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var workflowEntityList = workflowRepository.findByProfileIdOrderByCreatedAtAscIdAsc(profileId);
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, workflowEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, workflowEntityList.size());
        var workflowRecordList = workflowEntityList.subList(firstIndex, lastIndex).stream().map(this::toWorkflowRecord).toList();
        return new WorkflowPageRecord(workflowRecordList, normalizedPage, normalizedPageSize, workflowEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowRecord getWorkflowByIdAndProfileId(String workflowId, String profileId) {
        return toWorkflowRecord(findWorkflowByIdAndProfileId(workflowId, profileId));
    }

    @Override
    @Transactional
    public WorkflowRecord createWorkflowByProfileIdAndInput(String profileId, WorkflowInputRecord workflowInputRecord, String requestId) {
        validateWorkflowInput(workflowInputRecord, true);
        var profile = profileRepository.findById(profileId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, profileId));
        var workflow = new Workflow();
        workflow.setProfileId(profileId);
        applyWorkflowInput(workflow, workflowInputRecord, true);

        if (ValidateUtils.isValidString(requestId)) {
            var projectId = profile.getProjectId();
            var requestContent = workflowInputRecord.toString();
            var newWorkflowId = workflow.getId();
            var reservedWorkflowId = idempotencyKeyService.reserveResourceIdByRequest(projectId, "create_workflow", requestId, requestContent, newWorkflowId);

            if (!reservedWorkflowId.equals(workflow.getId())) {
                var existingWorkflowOptional = workflowRepository.findById(reservedWorkflowId);
                var existingWorkflow = existingWorkflowOptional.orElseThrow(() -> new HubException("CONFLICT", "error.conflict", 409, reservedWorkflowId));
                return toWorkflowRecord(existingWorkflow);
            }
        }

        return toWorkflowRecord(workflowRepository.save(workflow));
    }

    @Override
    @Transactional
    public WorkflowRecord updateWorkflowByIdAndProfileId(String workflowId, String profileId, WorkflowInputRecord workflowInputRecord, Long expectedVersion) {
        validateWorkflowInput(workflowInputRecord, false);
        var workflow = findWorkflowByIdAndProfileId(workflowId, profileId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion != workflow.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, workflowId);
        }

        applyWorkflowInput(workflow, workflowInputRecord, false);
        return toWorkflowRecord(workflowRepository.save(workflow));
    }

    @Override
    @Transactional
    public void deleteWorkflowByIdAndProfileId(String workflowId, String profileId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        workflowRepository.delete(findWorkflowByIdAndProfileId(workflowId, profileId));
    }

    private Workflow findWorkflowByIdAndProfileId(String workflowId, String profileId) {
        return workflowRepository.findByIdAndProfileId(workflowId, profileId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, workflowId));
    }

    private void validateWorkflowInput(WorkflowInputRecord workflowInputRecord, boolean create) {
        if (Objects.isNull(workflowInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "workflow");
        }

        if (!create && hasNoWorkflowChanges(workflowInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "changes");
        }

        if (create && (ValidateUtils.isInvalidString(workflowInputRecord.name()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        }

        if (create && (ValidateUtils.isInvalidString(workflowInputRecord.instructions()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "instructions");
        }

        if (!create && Objects.nonNull(workflowInputRecord.name()) && ValidateUtils.isInvalidString(workflowInputRecord.name())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        }

        if (!create && Objects.nonNull(workflowInputRecord.instructions()) && ValidateUtils.isInvalidString(workflowInputRecord.instructions())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "instructions");
        }
    }

    private void applyWorkflowInput(Workflow workflow, WorkflowInputRecord workflowInputRecord, boolean create) {
        if (create || Objects.nonNull(workflowInputRecord.name())) {
            workflow.setName(workflowInputRecord.name().strip());
        }

        if (create || Objects.nonNull(workflowInputRecord.description())) {
            workflow.setDescription(Objects.requireNonNullElse(workflowInputRecord.description(), ""));
        }

        if (create || Objects.nonNull(workflowInputRecord.instructions())) {
            workflow.setInstructions(workflowInputRecord.instructions());
        }

        if (Objects.nonNull(workflowInputRecord.enabled())) {
            workflow.setEnabled(workflowInputRecord.enabled());
        }
    }

    private boolean hasNoWorkflowChanges(WorkflowInputRecord workflowInputRecord) {
        return Objects.isNull(workflowInputRecord.name()) && Objects.isNull(workflowInputRecord.description()) && Objects.isNull(workflowInputRecord.instructions()) && Objects.isNull(workflowInputRecord.enabled());
    }

    private WorkflowRecord toWorkflowRecord(Workflow workflow) {
        return new WorkflowRecord(workflow.getId(), workflow.getProfileId(), workflow.getName(), workflow.getDescription(), workflow.getInstructions(), workflow.isEnabled(), workflow.getCreatedAt(), workflow.getUpdatedAt(), workflow.getVersion());
    }
}
