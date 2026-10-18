package io.agentmemoryhub.core.service.profile;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.record.ProfileInputRecord;
import io.agentmemoryhub.core.model.record.ProfilePageRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ProfileRepository profileRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfilePageRecord listProfilesByProjectId(String projectId, int page, int pageSize) {
        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var profileEntityList = profileRepository.findByProjectIdOrderByCreatedAtAscIdAsc(projectId);
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, profileEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, profileEntityList.size());
        var profileRecordList = profileEntityList.subList(firstIndex, lastIndex).stream().map(this::toProfileRecord).toList();
        return new ProfilePageRecord(profileRecordList, normalizedPage, normalizedPageSize, profileEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileRecord getProfileByIdAndProjectId(String profileId, String projectId) {
        var profile = findProfileByIdAndProjectId(profileId, projectId);
        return toProfileRecord(profile);
    }

    @Override
    @Transactional
    public ProfileRecord createProfileByProjectIdAndInput(String projectId, ProfileInputRecord profileInputRecord) {
        validateProfileInput(profileInputRecord);
        var project = projectRepository.findById(projectId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, projectId));

        if (!project.isEnabled()) {
            throw new HubException("PROJECT_INACTIVE", "error.projectInactive", 409, projectId);
        }

        var profile = new Profile();
        profile.setProjectId(projectId);
        profile.setName(profileInputRecord.name().strip());
        profile.setDescription(profileInputRecord.description());
        profile.setEnabled(Objects.isNull(profileInputRecord.enabled()) || profileInputRecord.enabled());
        return toProfileRecord(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public ProfileRecord updateProfileByIdAndProjectId(String profileId, String projectId, ProfileInputRecord profileInputRecord, Long expectedVersion) {
        validateProfileUpdateInput(profileInputRecord);
        var profile = findProfileByIdAndProjectId(profileId, projectId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion.longValue() != profile.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, profileId);
        }

        if (Objects.nonNull(profileInputRecord.name())) {
            profile.setName(profileInputRecord.name().strip());
        }

        if (Objects.nonNull(profileInputRecord.description())) {
            profile.setDescription(profileInputRecord.description());
        }

        if (Objects.nonNull(profileInputRecord.enabled())) {
            profile.setEnabled(profileInputRecord.enabled());
        }

        if (!profile.isEnabled()) {
            projectRepository.findById(projectId).ifPresent(project -> {
                if (profileId.equals(project.getActiveProfileId())) {
                    project.setActiveProfileId(null);
                    projectRepository.save(project);
                }
            });
        }

        return toProfileRecord(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public void deleteProfileByIdAndProjectId(String profileId, String projectId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        var profile = findProfileByIdAndProjectId(profileId, projectId);

        projectRepository.findById(projectId).ifPresent(project -> {
            if (profileId.equals(project.getActiveProfileId())) {
                project.setActiveProfileId(null);
                projectRepository.save(project);
            }
        });

        profileRepository.delete(profile);
    }

    private Profile findProfileByIdAndProjectId(String profileId, String projectId) {
        return profileRepository.findByIdAndProjectId(profileId, projectId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, profileId));
    }

    private void validateProfileInput(ProfileInputRecord profileInputRecord) {
        if (Objects.isNull(profileInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        }

        if (ValidateUtils.isInvalidString(profileInputRecord.name())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        }
    }

    private void validateProfileUpdateInput(ProfileInputRecord profileInputRecord) {
        if (hasNoProfileChanges(profileInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "changes");
        }

        if (Objects.nonNull(profileInputRecord.name()) && ValidateUtils.isInvalidString(profileInputRecord.name())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        }
    }

    private boolean hasNoProfileChanges(ProfileInputRecord profileInputRecord) {
        if (Objects.isNull(profileInputRecord)) {
            return true;
        }

        return Objects.isNull(profileInputRecord.name()) && Objects.isNull(profileInputRecord.description()) && Objects.isNull(profileInputRecord.enabled());
    }

    private ProfileRecord toProfileRecord(Profile profile) {
        return new ProfileRecord(profile.getId(), profile.getProjectId(), profile.getName(), profile.getDescription(), profile.isEnabled(), profile.getCreatedAt(), profile.getUpdatedAt(), profile.getVersion());
    }
}
