package io.agentmemoryhub.core.service.proposal;

import io.agentmemoryhub.core.common.ApplicationMode;
import io.agentmemoryhub.core.converter.TagsJsonDataConverter;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Memory;
import io.agentmemoryhub.core.model.domain.MemoryProposal;
import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalPageRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalRecord;
import io.agentmemoryhub.core.model.record.MemoryRecord;
import io.agentmemoryhub.core.repository.MemoryProposalRepository;
import io.agentmemoryhub.core.repository.MemoryRepository;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.TaskRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemoryProposalServiceImpl implements MemoryProposalService {
    private final MemoryProposalRepository memoryProposalRepository;
    private final MemoryRepository memoryRepository;
    private final TaskRepository taskRepository;
    private final ProfileRepository profileRepository;
    private final ProjectRepository projectRepository;
    private final TagsJsonDataConverter tagsJsonDataConverter;
    private final IdempotencyKeyService idempotencyKeyService;

    @Override
    @Transactional(readOnly = true)
    public MemoryProposalPageRecord listMemoryProposalsByTaskId(String taskId, int page, int pageSize) {
        if (!taskRepository.existsById(taskId)) {
            throw new HubException("NOT_FOUND", "error.notFound", 404, taskId);
        }

        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var memoryProposalEntityList = memoryProposalRepository.findByTaskIdOrderByCreatedAtAscIdAsc(taskId);
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, memoryProposalEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, memoryProposalEntityList.size());
        var memoryProposalRecordList = memoryProposalEntityList.subList(firstIndex, lastIndex).stream().map(this::toMemoryProposalRecord).toList();
        return new MemoryProposalPageRecord(memoryProposalRecordList, normalizedPage, normalizedPageSize, memoryProposalEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public MemoryProposalRecord getMemoryProposalById(String proposalId) {
        return toMemoryProposalRecord(findMemoryProposalById(proposalId));
    }

    @Override
    @Transactional
    public MemoryProposalRecord createMemoryProposalByTaskIdAndInput(String taskId, MemoryProposalInputRecord memoryProposalInputRecord, String requestId) {
        validateMemoryProposalInput(memoryProposalInputRecord, true);
        var task = taskRepository.findById(taskId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, taskId));
        var profile = profileRepository.findByIdAndEnabledTrue(task.getProfileId()).orElseThrow(() -> new HubException("PROFILE_INACTIVE", "error.profileInactive", 409, task.getProfileId()));
        var project = projectRepository.findById(profile.getProjectId()).orElseThrow(() -> new HubException("INVALID_RELATION", "error.invalidRelation", 409, taskId));

        if (!project.isEnabled() || !profile.getId().equals(project.getActiveProfileId())) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 409, taskId);
        }

        var memoryProposal = new MemoryProposal();
        memoryProposal.setTaskId(taskId);
        memoryProposal.setProposedContent(memoryProposalInputRecord.proposedContent());
        memoryProposal.setReason(memoryProposalInputRecord.reason());
        memoryProposal.setTagsJson(tagsJsonDataConverter.toJson(memoryProposalInputRecord.tags()));

        if (ValidateUtils.isValidString(requestId)) {
            var projectId = project.getId();
            var requestContent = memoryProposalInputRecord.toString();
            var newProposalId = memoryProposal.getId();
            var reservedProposalId = idempotencyKeyService.reserveResourceIdByRequest(projectId, "propose_memory", requestId, requestContent, newProposalId);

            if (!reservedProposalId.equals(memoryProposal.getId())) {
                var existingProposalOptional = memoryProposalRepository.findById(reservedProposalId);
                var existingProposal = existingProposalOptional.orElseThrow(() -> new HubException("CONFLICT", "error.conflict", 409, reservedProposalId));
                return toMemoryProposalRecord(existingProposal);
            }
        }

        return toMemoryProposalRecord(memoryProposalRepository.save(memoryProposal));
    }

    @Override
    @Transactional
    public MemoryProposalRecord updateMemoryProposalById(String proposalId, MemoryProposalInputRecord memoryProposalInputRecord, Long expectedVersion) {
        validateMemoryProposalInput(memoryProposalInputRecord, false);
        var memoryProposal = findMemoryProposalById(proposalId);
        validatePending(memoryProposal);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion.longValue() != memoryProposal.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, proposalId);
        }

        if (Objects.nonNull(memoryProposalInputRecord.proposedContent())) {
            memoryProposal.setProposedContent(memoryProposalInputRecord.proposedContent());
        }

        if (Objects.nonNull(memoryProposalInputRecord.reason())) {
            memoryProposal.setReason(memoryProposalInputRecord.reason());
        }

        if (Objects.nonNull(memoryProposalInputRecord.tags())) {
            memoryProposal.setTagsJson(tagsJsonDataConverter.toJson(memoryProposalInputRecord.tags()));
        }

        return toMemoryProposalRecord(memoryProposalRepository.save(memoryProposal));
    }

    @Override
    @Transactional
    public void deleteMemoryProposalById(String proposalId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        memoryProposalRepository.delete(findMemoryProposalById(proposalId));
    }

    @Override
    @Transactional
    public MemoryRecord approveMemoryProposalById(String proposalId, MemoryInputRecord memoryInputRecord) {
        var memoryProposal = findMemoryProposalById(proposalId);

        if (memoryProposal.isProcessed()) {
            if (Objects.isNull(memoryProposal.getResultMemoryId())) {
                throw new HubException("CONFLICT", "error.conflict", 409, proposalId);
            }

            var approvedMemoryOptional = memoryRepository.findById(memoryProposal.getResultMemoryId());
            var approvedMemoryRecordOptional = approvedMemoryOptional.map(this::toMemoryRecord);
            return approvedMemoryRecordOptional.orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, "resultMemoryId"));
        }

        validateMemoryInput(memoryInputRecord);
        var task = taskRepository.findById(memoryProposal.getTaskId()).orElseThrow(() -> new HubException("INVALID_RELATION", "error.invalidRelation", 409, proposalId));
        var profile = profileRepository.findById(task.getProfileId()).orElseThrow(() -> new HubException("INVALID_RELATION", "error.invalidRelation", 409, proposalId));
        var memory = new Memory();
        memory.setProfileId(profile.getId());
        memory.setTitle(memoryInputRecord.title().strip());
        memory.setContent(Objects.requireNonNullElse(memoryInputRecord.content(), memoryProposal.getProposedContent()));
        var tagsList = Objects.nonNull(memoryInputRecord.tags()) ? memoryInputRecord.tags() : tagsJsonDataConverter.fromJson(memoryProposal.getTagsJson());
        memory.setTagsJson(tagsJsonDataConverter.toJson(tagsList));
        memory.setApplicationMode(Objects.requireNonNullElse(memoryInputRecord.applicationMode(), ApplicationMode.CONTEXTUAL));
        memory.setEnabled(Objects.isNull(memoryInputRecord.enabled()) || memoryInputRecord.enabled());
        var savedMemory = memoryRepository.save(memory);
        memoryProposal.setProcessed(true);
        memoryProposal.setResultMemoryId(savedMemory.getId());
        memoryProposalRepository.save(memoryProposal);
        return toMemoryRecord(savedMemory);
    }

    private MemoryProposal findMemoryProposalById(String proposalId) {
        return memoryProposalRepository.findById(proposalId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, proposalId));
    }

    private void validatePending(MemoryProposal memoryProposal) {
        if (memoryProposal.isProcessed()) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }
    }

    private void validateMemoryProposalInput(MemoryProposalInputRecord memoryProposalInputRecord, boolean create) {
        if (Objects.isNull(memoryProposalInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "proposal");
        }

        if (!create && hasNoMemoryProposalChanges(memoryProposalInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "changes");
        }

        if (create && (ValidateUtils.isInvalidString(memoryProposalInputRecord.proposedContent()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "proposedContent");
        }

        if (create && (ValidateUtils.isInvalidString(memoryProposalInputRecord.reason()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "reason");
        }

        if (!create && Objects.nonNull(memoryProposalInputRecord.proposedContent()) && ValidateUtils.isInvalidString(memoryProposalInputRecord.proposedContent())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "proposedContent");
        }

        if (!create && Objects.nonNull(memoryProposalInputRecord.reason()) && ValidateUtils.isInvalidString(memoryProposalInputRecord.reason())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "reason");
        }
    }

    private void validateMemoryInput(MemoryInputRecord memoryInputRecord) {
        if (Objects.isNull(memoryInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "title");
        }

        if (ValidateUtils.isInvalidString(memoryInputRecord.title())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "title");
        }

        if (Objects.nonNull(memoryInputRecord.content()) && ValidateUtils.isInvalidString(memoryInputRecord.content())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "content");
        }
    }

    private boolean hasNoMemoryProposalChanges(MemoryProposalInputRecord memoryProposalInputRecord) {
        return Objects.isNull(memoryProposalInputRecord.proposedContent()) && Objects.isNull(memoryProposalInputRecord.reason()) && Objects.isNull(memoryProposalInputRecord.tags());
    }

    private MemoryProposalRecord toMemoryProposalRecord(MemoryProposal memoryProposal) {
        return new MemoryProposalRecord(memoryProposal.getId(), memoryProposal.getTaskId(), memoryProposal.getProposedContent(), memoryProposal.getReason(), tagsJsonDataConverter.fromJson(memoryProposal.getTagsJson()), memoryProposal.isProcessed(), memoryProposal.getResultMemoryId(), memoryProposal.getCreatedAt(), memoryProposal.getUpdatedAt(), memoryProposal.getVersion());
    }

    private MemoryRecord toMemoryRecord(Memory memory) {
        return new MemoryRecord(memory.getId(), memory.getProfileId(), memory.getTitle(), memory.getContent(), tagsJsonDataConverter.fromJson(memory.getTagsJson()), memory.getApplicationMode(), memory.isEnabled(), memory.getCreatedAt(), memory.getUpdatedAt(), memory.getVersion());
    }
}
