package io.agentmemoryhub.core.service.settings;

import io.agentmemoryhub.core.model.record.ProjectSettingsInputRecord;
import io.agentmemoryhub.core.model.record.ProjectSettingsRecord;

public interface ProjectSettingsService {
    ProjectSettingsRecord getProjectSettingsByProjectId(String projectId);

    ProjectSettingsRecord updateProjectSettingsByProjectId(String projectId, ProjectSettingsInputRecord projectSettingsInputRecord, Long expectedVersion);

    ProjectSettingsRecord resetProjectSettingsByProjectId(String projectId);
}
