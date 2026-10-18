package io.agentmemoryhub.core.service.member;

import io.agentmemoryhub.core.common.ProjectRole;
import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.ProjectMemberInputRecord;
import io.agentmemoryhub.core.model.record.ProjectMemberPageRecord;
import io.agentmemoryhub.core.model.record.ProjectMemberRecord;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.UserRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {
    private final ProjectMemberDAO projectMemberDAO;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ProjectMemberPageRecord listProjectMembersByProjectId(String projectId) {
        validateProjectExists(projectId);
        var projectMemberRecordList = projectMemberDAO.findProjectMembersByProjectId(projectId);
        return new ProjectMemberPageRecord(projectMemberRecordList, 0, projectMemberRecordList.size(), projectMemberRecordList.size());
    }

    @Override
    @Transactional
    public ProjectMemberRecord addProjectMemberByProjectIdAndInput(String projectId, ProjectMemberInputRecord inputRecord) {
        validateProjectExists(projectId);
        validateProjectMemberInput(inputRecord);

        if (inputRecord.projectRole() == ProjectRole.OWNER) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        if (!userRepository.existsById(inputRecord.userId())) {
            throw new HubException("NOT_FOUND", "error.notFound", 404, inputRecord.userId());
        }

        projectMemberDAO.addProjectMember(projectId, inputRecord.userId(), inputRecord.projectRole());
        return findProjectMemberByProjectIdAndUserId(projectId, inputRecord.userId());
    }

    @Override
    @Transactional
    public ProjectMemberRecord updateProjectMemberByProjectIdAndUserId(String projectId, String userId, ProjectMemberInputRecord inputRecord) {
        validateProjectExists(projectId);
        validateProjectMemberInput(inputRecord);

        if (Objects.nonNull(inputRecord.userId()) && !userId.equals(inputRecord.userId())) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        if (inputRecord.projectRole() == ProjectRole.OWNER) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        projectMemberDAO.updateProjectRoleByProjectIdAndUserId(projectId, userId, inputRecord.projectRole());
        return findProjectMemberByProjectIdAndUserId(projectId, userId);
    }

    @Override
    @Transactional
    public void removeProjectMemberByProjectIdAndUserId(String projectId, String userId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        validateProjectExists(projectId);
        projectMemberDAO.removeProjectMemberByProjectIdAndUserId(projectId, userId);
    }

    private void validateProjectExists(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new HubException("NOT_FOUND", "error.notFound", 404, projectId);
        }
    }

    private void validateProjectMemberInput(ProjectMemberInputRecord inputRecord) {
        if (Objects.isNull(inputRecord) || Objects.isNull(inputRecord.projectRole())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "projectRole");
        }
    }

    private ProjectMemberRecord findProjectMemberByProjectIdAndUserId(String projectId, String userId) {
        var projectMemberRecordList = projectMemberDAO.findProjectMembersByProjectId(projectId);
        var projectMemberOptional = projectMemberRecordList.stream().filter(projectMemberRecord -> projectMemberRecord.userId().equals(userId)).findFirst();
        return projectMemberOptional.orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, userId));
    }
}
