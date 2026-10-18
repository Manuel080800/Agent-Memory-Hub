package io.agentmemoryhub.core.service.member;

import io.agentmemoryhub.core.model.record.ProjectMemberInputRecord;
import io.agentmemoryhub.core.model.record.ProjectMemberPageRecord;
import io.agentmemoryhub.core.model.record.ProjectMemberRecord;

public interface ProjectMemberService {
    ProjectMemberPageRecord listProjectMembersByProjectId(String projectId);

    ProjectMemberRecord addProjectMemberByProjectIdAndInput(String projectId, ProjectMemberInputRecord projectMemberInputRecord);

    ProjectMemberRecord updateProjectMemberByProjectIdAndUserId(String projectId, String userId, ProjectMemberInputRecord projectMemberInputRecord);

    void removeProjectMemberByProjectIdAndUserId(String projectId, String userId, boolean confirm);
}
