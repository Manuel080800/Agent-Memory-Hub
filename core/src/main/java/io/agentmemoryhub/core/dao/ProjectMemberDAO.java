package io.agentmemoryhub.core.dao;

import io.agentmemoryhub.core.common.ProjectRole;
import io.agentmemoryhub.core.model.record.ProjectMemberRecord;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberDAO {
    void addOwnerMember(String projectId, String userId);

    void addProjectMember(String projectId, String userId, ProjectRole projectRole);

    List<ProjectMemberRecord> findProjectMembersByProjectId(String projectId);

    List<String> findProjectIdsByUserId(String userId);

    Optional<ProjectRole> findProjectRoleByProjectIdAndUserId(String projectId, String userId);

    void updateProjectRoleByProjectIdAndUserId(String projectId, String userId, ProjectRole projectRole);

    void removeProjectMemberByProjectIdAndUserId(String projectId, String userId);
}
