package io.agentmemoryhub.core.service.skill;

import io.agentmemoryhub.core.common.ApplicationMode;
import io.agentmemoryhub.core.converter.TagsJsonDataConverter;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Skill;
import io.agentmemoryhub.core.model.record.SkillInputRecord;
import io.agentmemoryhub.core.model.record.SkillPageRecord;
import io.agentmemoryhub.core.model.record.SkillRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.SkillRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {
    private final SkillRepository skillRepository;
    private final ProfileRepository profileRepository;
    private final TagsJsonDataConverter tagsJsonDataConverter;
    private final IdempotencyKeyService idempotencyKeyService;

    @Override
    @Transactional(readOnly = true)
    public SkillPageRecord listSkillsByProfileId(String profileId, int page, int pageSize) {
        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var skillEntityList = skillRepository.findByProfileIdOrderByCreatedAtAscIdAsc(profileId);
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, skillEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, skillEntityList.size());
        var skillRecordList = skillEntityList.subList(firstIndex, lastIndex).stream().map(this::toSkillRecord).toList();
        return new SkillPageRecord(skillRecordList, normalizedPage, normalizedPageSize, skillEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public SkillRecord getSkillByIdAndProfileId(String skillId, String profileId) {
        return toSkillRecord(findSkillByIdAndProfileId(skillId, profileId));
    }

    @Override
    @Transactional
    public SkillRecord createSkillByProfileIdAndInput(String profileId, SkillInputRecord skillInputRecord, String requestId) {
        validateSkillInput(skillInputRecord, true);
        var profile = profileRepository.findById(profileId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, profileId));
        var skill = new Skill();
        skill.setProfileId(profileId);
        applySkillInput(skill, skillInputRecord, true);

        if (ValidateUtils.isValidString(requestId)) {
            var projectId = profile.getProjectId();
            var requestContent = skillInputRecord.toString();
            var reservedSkillId = idempotencyKeyService.reserveResourceIdByRequest(projectId, "create_skill", requestId, requestContent, skill.getId());

            if (!reservedSkillId.equals(skill.getId())) {
                var existingSkillOptional = skillRepository.findById(reservedSkillId);
                var existingSkill = existingSkillOptional.orElseThrow(() -> new HubException("CONFLICT", "error.conflict", 409, reservedSkillId));
                return toSkillRecord(existingSkill);
            }
        }

        return toSkillRecord(skillRepository.save(skill));
    }

    @Override
    @Transactional
    public SkillRecord updateSkillByIdAndProfileId(String skillId, String profileId, SkillInputRecord skillInputRecord, Long expectedVersion) {
        validateSkillInput(skillInputRecord, false);
        var skill = findSkillByIdAndProfileId(skillId, profileId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion.longValue() != skill.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, skillId);
        }

        applySkillInput(skill, skillInputRecord, false);
        return toSkillRecord(skillRepository.save(skill));
    }

    @Override
    @Transactional
    public void deleteSkillByIdAndProfileId(String skillId, String profileId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        skillRepository.delete(findSkillByIdAndProfileId(skillId, profileId));
    }

    private Skill findSkillByIdAndProfileId(String skillId, String profileId) {
        return skillRepository.findByIdAndProfileId(skillId, profileId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, skillId));
    }

    private void validateSkillInput(SkillInputRecord skillInputRecord, boolean create) {
        if (Objects.isNull(skillInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "skill");
        }

        if (create && (ValidateUtils.isInvalidString(skillInputRecord.name()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        }

        if (create && (ValidateUtils.isInvalidString(skillInputRecord.instructions()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "instructions");
        }
    }

    private void applySkillInput(Skill skill, SkillInputRecord skillInputRecord, boolean create) {
        if (create || Objects.nonNull(skillInputRecord.name())) {
            skill.setName(skillInputRecord.name().strip());
        }

        if (create || Objects.nonNull(skillInputRecord.description())) {
            skill.setDescription(Objects.requireNonNullElse(skillInputRecord.description(), ""));
        }

        if (create || Objects.nonNull(skillInputRecord.instructions())) {
            skill.setInstructions(skillInputRecord.instructions());
        }

        if (create || Objects.nonNull(skillInputRecord.tags())) {
            skill.setTagsJson(tagsJsonDataConverter.toJson(skillInputRecord.tags()));
        }

        if (create || Objects.nonNull(skillInputRecord.applicationMode())) {
            skill.setApplicationMode(Objects.requireNonNullElse(skillInputRecord.applicationMode(), ApplicationMode.CONTEXTUAL));
        }

        if (Objects.nonNull(skillInputRecord.enabled())) {
            skill.setEnabled(skillInputRecord.enabled());
        }
    }

    private SkillRecord toSkillRecord(Skill skill) {
        return new SkillRecord(skill.getId(), skill.getProfileId(), skill.getName(), skill.getDescription(), skill.getInstructions(), tagsJsonDataConverter.fromJson(skill.getTagsJson()), skill.getApplicationMode(), skill.isEnabled(), skill.getCreatedAt(), skill.getUpdatedAt(), skill.getVersion());
    }
}
