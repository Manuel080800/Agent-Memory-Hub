package io.agentmemoryhub.core.service.project;

import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Project;
import io.agentmemoryhub.core.model.domain.ProjectSettings;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.ProjectInputRecord;
import io.agentmemoryhub.core.model.record.ProjectPageRecord;
import io.agentmemoryhub.core.model.record.ProjectRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.ProjectSettingsRepository;
import io.agentmemoryhub.core.repository.UserRepository;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.util.Comparator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectSettingsRepository projectSettingsRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProjectMemberDAO projectMemberDAO;

    @Override
    @Transactional(readOnly = true)
    public ProjectPageRecord listProjects(int page, int pageSize) {
        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var projectEntityList = projectRepository.findAll().stream().sorted(Comparator.comparing(Project::getCreatedAt).reversed().thenComparing(Project::getId)).toList();
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, projectEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, projectEntityList.size());
        var projectRecordList = projectEntityList.subList(firstIndex, lastIndex).stream().map(this::toProjectRecord).toList();
        return new ProjectPageRecord(projectRecordList, normalizedPage, normalizedPageSize, projectEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectPageRecord listProjectsByUserId(String userId, int page, int pageSize) {
        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var projectIdList = projectMemberDAO.findProjectIdsByUserId(userId);
        var projectEntityList = projectRepository.findAllById(projectIdList).stream().sorted(Comparator.comparing(Project::getCreatedAt).reversed().thenComparing(Project::getId)).toList();
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, projectEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, projectEntityList.size());
        var projectRecordList = projectEntityList.subList(firstIndex, lastIndex).stream().map(this::toProjectRecord).toList();
        return new ProjectPageRecord(projectRecordList, normalizedPage, normalizedPageSize, projectEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectRecord getProjectById(String projectId) {
        var project = findProjectById(projectId);
        return toProjectRecord(project);
    }

    @Override
    @Transactional
    public ProjectRecord createProjectByInput(ProjectInputRecord projectInputRecord) {
        validateProjectInput(projectInputRecord);
        var ownerUserId = projectInputRecord.ownerUserId();

        if (Objects.isNull(ownerUserId) || !userRepository.existsById(ownerUserId)) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, "ownerUserId");
        }

        var project = new Project();
        project.setName(projectInputRecord.name().strip());
        project.setDescription(projectInputRecord.description());
        project.setEnabled(Objects.isNull(projectInputRecord.enabled()) || projectInputRecord.enabled());
        project.setOwnerUserId(ownerUserId);
        var savedProject = projectRepository.saveAndFlush(project);
        var projectSettings = new ProjectSettings();
        projectSettings.setProjectId(savedProject.getId());
        projectSettingsRepository.save(projectSettings);
        projectMemberDAO.addOwnerMember(savedProject.getId(), ownerUserId);
        return toProjectRecord(savedProject);
    }

    @Override
    @Transactional
    public ProjectRecord updateProjectById(String projectId, ProjectInputRecord projectInputRecord, Long expectedVersion) {
        validateProjectUpdateInput(projectInputRecord);
        var project = findProjectById(projectId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion != project.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, projectId);
        }

        if (Objects.nonNull(projectInputRecord.ownerUserId())) {
            if (!project.getOwnerUserId().equals(projectInputRecord.ownerUserId())) {
                throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
            }
        }

        if (Objects.nonNull(projectInputRecord.name())) {
            project.setName(projectInputRecord.name().strip());
        }

        if (Objects.nonNull(projectInputRecord.description())) {
            project.setDescription(projectInputRecord.description());
        }

        if (Objects.nonNull(projectInputRecord.enabled())) {
            project.setEnabled(projectInputRecord.enabled());
        }

        return toProjectRecord(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void deleteProjectById(String projectId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        var project = findProjectById(projectId);
        projectRepository.delete(project);
    }

    @Override
    @Transactional
    public ProfileRecord selectActiveProfileByProjectIdAndProfileId(String projectId, String profileId) {
        var project = findProjectById(projectId);
        var profile = profileRepository.findByIdAndProjectIdAndEnabledTrue(profileId, projectId).orElseThrow(() -> new HubException("INVALID_RELATION", "error.invalidRelation", 422, profileId));
        project.setActiveProfileId(profile.getId());
        projectRepository.save(project);
        return new ProfileRecord(profile.getId(), profile.getProjectId(), profile.getName(), profile.getDescription(), profile.isEnabled(), profile.getCreatedAt(), profile.getUpdatedAt(), profile.getVersion());
    }

    private Project findProjectById(String projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, projectId));
    }

    private void validateProjectInput(ProjectInputRecord projectInputRecord) {
        if (Objects.isNull(projectInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        }

        if (ValidateUtils.isInvalidString(projectInputRecord.name())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        }
    }

    private void validateProjectUpdateInput(ProjectInputRecord projectInputRecord) {
        if (hasNoProjectChanges(projectInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "changes");
        }

        if (Objects.nonNull(projectInputRecord.name()) && ValidateUtils.isInvalidString(projectInputRecord.name())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        }
    }

    private boolean hasNoProjectChanges(ProjectInputRecord projectInputRecord) {
        if (Objects.isNull(projectInputRecord)) {
            return true;
        }

        return Objects.isNull(projectInputRecord.name()) && Objects.isNull(projectInputRecord.description()) && Objects.isNull(projectInputRecord.enabled()) && Objects.isNull(projectInputRecord.ownerUserId());
    }

    private ProjectRecord toProjectRecord(Project project) {
        return new ProjectRecord(project.getId(), project.getName(), project.getDescription(), project.isEnabled(), project.getOwnerUserId(), project.getActiveProfileId(), project.getCreatedAt(), project.getUpdatedAt(), project.getVersion());
    }
}
