package io.agentmemoryhub.core.service.project;

import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.ProjectInputRecord;
import io.agentmemoryhub.core.model.record.ProjectPageRecord;
import io.agentmemoryhub.core.model.record.ProjectRecord;

public interface ProjectService {
    ProjectPageRecord listProjects(int page, int pageSize);

    ProjectPageRecord listProjectsByUserId(String userId, int page, int pageSize);

    ProjectRecord getProjectById(String projectId);

    ProjectRecord createProjectByInput(ProjectInputRecord projectInputRecord);

    ProjectRecord updateProjectById(String projectId, ProjectInputRecord projectInputRecord, Long expectedVersion);

    void deleteProjectById(String projectId, boolean confirm);

    ProfileRecord selectActiveProfileByProjectIdAndProfileId(String projectId, String profileId);
}
