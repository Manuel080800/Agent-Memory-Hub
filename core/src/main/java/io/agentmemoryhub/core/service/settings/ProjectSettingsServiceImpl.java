package io.agentmemoryhub.core.service.settings;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.ProjectSettings;
import io.agentmemoryhub.core.model.record.ProjectSettingsInputRecord;
import io.agentmemoryhub.core.model.record.ProjectSettingsRecord;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.ProjectSettingsRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectSettingsServiceImpl implements ProjectSettingsService {
    private final ProjectRepository projectRepository;
    private final ProjectSettingsRepository projectSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public ProjectSettingsRecord getProjectSettingsByProjectId(String projectId) {
        validateProjectExists(projectId);
        return toProjectSettingsRecord(findProjectSettingsByProjectId(projectId));
    }

    @Override
    @Transactional
    public ProjectSettingsRecord updateProjectSettingsByProjectId(String projectId, ProjectSettingsInputRecord projectSettingsInputRecord, Long expectedVersion) {
        validateProjectExists(projectId);
        validateProjectSettingsInput(projectSettingsInputRecord);
        var projectSettings = findProjectSettingsByProjectId(projectId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion != projectSettings.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, projectId);
        }

        applyProjectSettingsInput(projectSettings, projectSettingsInputRecord);
        return toProjectSettingsRecord(projectSettingsRepository.save(projectSettings));
    }

    @Override
    @Transactional
    public ProjectSettingsRecord resetProjectSettingsByProjectId(String projectId) {
        validateProjectExists(projectId);
        var defaultProjectSettings = new ProjectSettings();
        var projectSettings = findProjectSettingsByProjectId(projectId);
        projectSettings.setProjectId(projectId);
        projectSettings.setContextRetrievalEnabled(defaultProjectSettings.isContextRetrievalEnabled());
        projectSettings.setCompactRecoveryEnabled(defaultProjectSettings.isCompactRecoveryEnabled());
        projectSettings.setContextualSelectionEnabled(defaultProjectSettings.isContextualSelectionEnabled());
        projectSettings.setTaskCreationMode(defaultProjectSettings.getTaskCreationMode());
        projectSettings.setAutomaticSummaryEnabled(defaultProjectSettings.isAutomaticSummaryEnabled());
        projectSettings.setSelectedTaskRetrievalEnabled(defaultProjectSettings.isSelectedTaskRetrievalEnabled());
        projectSettings.setContextBudgetTokens(defaultProjectSettings.getContextBudgetTokens());
        projectSettings.setPermanentQuotaPercent(defaultProjectSettings.getPermanentQuotaPercent());
        projectSettings.setPreToolUseContextEnabled(defaultProjectSettings.isPreToolUseContextEnabled());
        return toProjectSettingsRecord(projectSettingsRepository.save(projectSettings));
    }

    private ProjectSettings findProjectSettingsByProjectId(String projectId) {
        return projectSettingsRepository.findById(projectId).orElseGet(() -> {
            var projectSettings = new ProjectSettings();
            projectSettings.setProjectId(projectId);
            return projectSettings;
        });
    }

    private void validateProjectExists(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new HubException("NOT_FOUND", "error.notFound", 404, projectId);
        }
    }

    private void validateProjectSettingsInput(ProjectSettingsInputRecord projectSettingsInputRecord) {
        if (hasNoProjectSettingsChanges(projectSettingsInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "changes");
        }

        if (isContextBudgetOutOfRange(projectSettingsInputRecord.contextBudgetTokens())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "contextBudgetTokens");
        }

        if (isPermanentQuotaOutOfRange(projectSettingsInputRecord.permanentQuotaPercent())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "permanentQuotaPercent");
        }
    }

    private boolean hasNoProjectSettingsChanges(ProjectSettingsInputRecord projectSettingsInputRecord) {
        if (Objects.isNull(projectSettingsInputRecord)) {
            return true;
        }

        var retrievalSettingsAreMissing = Objects.isNull(projectSettingsInputRecord.contextRetrievalEnabled()) && Objects.isNull(projectSettingsInputRecord.contextualSelectionEnabled()) && Objects.isNull(projectSettingsInputRecord.preToolUseContextEnabled());
        var taskSettingsAreMissing = Objects.isNull(projectSettingsInputRecord.compactRecoveryEnabled()) && Objects.isNull(projectSettingsInputRecord.taskCreationMode()) && Objects.isNull(projectSettingsInputRecord.automaticSummaryEnabled()) && Objects.isNull(projectSettingsInputRecord.selectedTaskRetrievalEnabled());
        var budgetSettingsAreMissing = Objects.isNull(projectSettingsInputRecord.contextBudgetTokens()) && Objects.isNull(projectSettingsInputRecord.permanentQuotaPercent());

        return retrievalSettingsAreMissing && taskSettingsAreMissing && budgetSettingsAreMissing;
    }

    private boolean isContextBudgetOutOfRange(Integer contextBudgetTokens) {
        if (Objects.isNull(contextBudgetTokens)) {
            return false;
        }

        return contextBudgetTokens < 100 || contextBudgetTokens > 100_000;
    }

    private boolean isPermanentQuotaOutOfRange(Integer permanentQuotaPercent) {
        if (Objects.isNull(permanentQuotaPercent)) {
            return false;
        }

        return permanentQuotaPercent < 0 || permanentQuotaPercent > 100;
    }

    private void applyProjectSettingsInput(ProjectSettings projectSettings, ProjectSettingsInputRecord inputRecord) {
        if (Objects.nonNull(inputRecord.contextRetrievalEnabled())) {
            projectSettings.setContextRetrievalEnabled(inputRecord.contextRetrievalEnabled());
        }

        if (Objects.nonNull(inputRecord.compactRecoveryEnabled())) {
            projectSettings.setCompactRecoveryEnabled(inputRecord.compactRecoveryEnabled());
        }

        if (Objects.nonNull(inputRecord.contextualSelectionEnabled())) {
            projectSettings.setContextualSelectionEnabled(inputRecord.contextualSelectionEnabled());
        }

        if (Objects.nonNull(inputRecord.taskCreationMode())) {
            projectSettings.setTaskCreationMode(inputRecord.taskCreationMode());
        }

        if (Objects.nonNull(inputRecord.automaticSummaryEnabled())) {
            projectSettings.setAutomaticSummaryEnabled(inputRecord.automaticSummaryEnabled());
        }

        if (Objects.nonNull(inputRecord.selectedTaskRetrievalEnabled())) {
            projectSettings.setSelectedTaskRetrievalEnabled(inputRecord.selectedTaskRetrievalEnabled());
        }

        if (Objects.nonNull(inputRecord.contextBudgetTokens())) {
            projectSettings.setContextBudgetTokens(inputRecord.contextBudgetTokens());
        }

        if (Objects.nonNull(inputRecord.permanentQuotaPercent())) {
            projectSettings.setPermanentQuotaPercent(inputRecord.permanentQuotaPercent());
        }

        if (Objects.nonNull(inputRecord.preToolUseContextEnabled())) {
            projectSettings.setPreToolUseContextEnabled(inputRecord.preToolUseContextEnabled());
        }
    }

    private ProjectSettingsRecord toProjectSettingsRecord(ProjectSettings projectSettings) {
        return new ProjectSettingsRecord(projectSettings.getProjectId(), projectSettings.isContextRetrievalEnabled(), projectSettings.isCompactRecoveryEnabled(), projectSettings.isContextualSelectionEnabled(), projectSettings.getTaskCreationMode(), projectSettings.isAutomaticSummaryEnabled(), projectSettings.isSelectedTaskRetrievalEnabled(), projectSettings.getContextBudgetTokens(), projectSettings.getPermanentQuotaPercent(), projectSettings.isPreToolUseContextEnabled(), projectSettings.getUpdatedAt(), projectSettings.getVersion());
    }
}
