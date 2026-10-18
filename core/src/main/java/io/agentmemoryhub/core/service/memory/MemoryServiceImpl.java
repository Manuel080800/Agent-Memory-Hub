package io.agentmemoryhub.core.service.memory;

import io.agentmemoryhub.core.common.ApplicationMode;
import io.agentmemoryhub.core.converter.TagsJsonDataConverter;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Memory;
import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.MemoryPageRecord;
import io.agentmemoryhub.core.model.record.MemoryRecord;
import io.agentmemoryhub.core.repository.MemoryRepository;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {
    private final MemoryRepository memoryRepository;
    private final ProfileRepository profileRepository;
    private final TagsJsonDataConverter tagsJsonDataConverter;
    private final IdempotencyKeyService idempotencyKeyService;

    @Override
    @Transactional(readOnly = true)
    public MemoryPageRecord listMemoriesByProfileId(String profileId, int page, int pageSize) {
        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var memoryEntityList = memoryRepository.findByProfileIdOrderByCreatedAtAscIdAsc(profileId);
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, memoryEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, memoryEntityList.size());
        var memoryRecordList = memoryEntityList.subList(firstIndex, lastIndex).stream().map(this::toMemoryRecord).toList();
        return new MemoryPageRecord(memoryRecordList, normalizedPage, normalizedPageSize, memoryEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public MemoryRecord getMemoryByIdAndProfileId(String memoryId, String profileId) {
        return toMemoryRecord(findMemoryByIdAndProfileId(memoryId, profileId));
    }

    @Override
    @Transactional
    public MemoryRecord createMemoryByProfileIdAndInput(String profileId, MemoryInputRecord memoryInputRecord, String requestId) {
        validateMemoryInput(memoryInputRecord, true);
        var profile = profileRepository.findById(profileId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, profileId));
        var memory = new Memory();
        memory.setProfileId(profileId);
        applyMemoryInput(memory, memoryInputRecord, true);

        if (ValidateUtils.isValidString(requestId)) {
            var projectId = profile.getProjectId();
            var requestContent = memoryInputRecord.toString();
            var reservedMemoryId = idempotencyKeyService.reserveResourceIdByRequest(projectId, "create_memory", requestId, requestContent, memory.getId());

            if (!reservedMemoryId.equals(memory.getId())) {
                var existingMemoryOptional = memoryRepository.findById(reservedMemoryId);
                var existingMemory = existingMemoryOptional.orElseThrow(() -> new HubException("CONFLICT", "error.conflict", 409, reservedMemoryId));
                return toMemoryRecord(existingMemory);
            }
        }

        return toMemoryRecord(memoryRepository.save(memory));
    }

    @Override
    @Transactional
    public MemoryRecord updateMemoryByIdAndProfileId(String memoryId, String profileId, MemoryInputRecord memoryInputRecord, Long expectedVersion) {
        validateMemoryInput(memoryInputRecord, false);
        var memory = findMemoryByIdAndProfileId(memoryId, profileId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion != memory.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, memoryId);
        }

        applyMemoryInput(memory, memoryInputRecord, false);
        return toMemoryRecord(memoryRepository.save(memory));
    }

    @Override
    @Transactional
    public void deleteMemoryByIdAndProfileId(String memoryId, String profileId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        memoryRepository.delete(findMemoryByIdAndProfileId(memoryId, profileId));
    }

    private Memory findMemoryByIdAndProfileId(String memoryId, String profileId) {
        return memoryRepository.findByIdAndProfileId(memoryId, profileId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, memoryId));
    }

    private void validateMemoryInput(MemoryInputRecord memoryInputRecord, boolean create) {
        if (Objects.isNull(memoryInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "memory");
        }

        if (!create && hasNoMemoryChanges(memoryInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "changes");
        }

        if (create && (ValidateUtils.isInvalidString(memoryInputRecord.title()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "title");
        }

        if (create && (ValidateUtils.isInvalidString(memoryInputRecord.content()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "content");
        }

        if (!create && Objects.nonNull(memoryInputRecord.title()) && ValidateUtils.isInvalidString(memoryInputRecord.title())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "title");
        }

        if (!create && Objects.nonNull(memoryInputRecord.content()) && ValidateUtils.isInvalidString(memoryInputRecord.content())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "content");
        }
    }

    private void applyMemoryInput(Memory memory, MemoryInputRecord memoryInputRecord, boolean create) {
        if (create || Objects.nonNull(memoryInputRecord.title())) {
            memory.setTitle(memoryInputRecord.title().strip());
        }

        if (create || Objects.nonNull(memoryInputRecord.content())) {
            memory.setContent(memoryInputRecord.content());
        }

        if (create || Objects.nonNull(memoryInputRecord.tags())) {
            memory.setTagsJson(tagsJsonDataConverter.toJson(memoryInputRecord.tags()));
        }

        if (create || Objects.nonNull(memoryInputRecord.applicationMode())) {
            memory.setApplicationMode(Objects.requireNonNullElse(memoryInputRecord.applicationMode(), ApplicationMode.CONTEXTUAL));
        }

        if (Objects.nonNull(memoryInputRecord.enabled())) {
            memory.setEnabled(memoryInputRecord.enabled());
        }
    }

    private boolean hasNoMemoryChanges(MemoryInputRecord memoryInputRecord) {
        return Objects.isNull(memoryInputRecord.title()) && Objects.isNull(memoryInputRecord.content()) && Objects.isNull(memoryInputRecord.tags()) && Objects.isNull(memoryInputRecord.applicationMode()) && Objects.isNull(memoryInputRecord.enabled());
    }

    private MemoryRecord toMemoryRecord(Memory memory) {
        return new MemoryRecord(memory.getId(), memory.getProfileId(), memory.getTitle(), memory.getContent(), tagsJsonDataConverter.fromJson(memory.getTagsJson()), memory.getApplicationMode(), memory.isEnabled(), memory.getCreatedAt(), memory.getUpdatedAt(), memory.getVersion());
    }
}
