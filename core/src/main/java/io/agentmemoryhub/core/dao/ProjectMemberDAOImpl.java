package io.agentmemoryhub.core.dao;

import io.agentmemoryhub.core.common.ProjectRole;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.ProjectMemberRecord;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectMemberDAOImpl implements ProjectMemberDAO {
    private final NamedParameterJdbcTemplate jdbcNativeTemplate;

    @Override
    public void addOwnerMember(String projectId, String userId) {
        var sqlCommand = buildSqlCommandForOwnerMember();
        var params = buildParamsForProjectAndUser(projectId, userId);
        jdbcNativeTemplate.update(sqlCommand, params);
    }

    @Override
    public void addProjectMember(String projectId, String userId, ProjectRole projectRole) {
        var sqlCommand = "INSERT INTO project_members (project_id, user_id, project_role) VALUES (:projectId, :userId, :projectRole)";
        var params = buildParamsForProjectAndUser(projectId, userId).addValue("projectRole", projectRole.name());
        jdbcNativeTemplate.update(sqlCommand, params);
    }

    @Override
    public List<ProjectMemberRecord> findProjectMembersByProjectId(String projectId) {
        var sqlCommand = buildSqlCommandForProjectMembersByProjectId();
        var params = new MapSqlParameterSource().addValue("projectId", projectId);
        return jdbcNativeTemplate.query(sqlCommand, params, (resultSet, rowNumber) -> new ProjectMemberRecord(resultSet.getString("project_id"), resultSet.getString("user_id"), ProjectRole.valueOf(resultSet.getString("project_role")), resultSet.getString("full_name"), resultSet.getString("email"), resultSet.getTimestamp("created_at").toInstant()));
    }

    @Override
    public List<String> findProjectIdsByUserId(String userId) {
        var sqlCommand = "SELECT project_id FROM project_members WHERE user_id = :userId ORDER BY created_at, project_id";
        var params = new MapSqlParameterSource().addValue("userId", userId);
        return jdbcNativeTemplate.query(sqlCommand, params, (resultSet, rowNumber) -> resultSet.getString("project_id"));
    }

    @Override
    public Optional<ProjectRole> findProjectRoleByProjectIdAndUserId(String projectId, String userId) {
        var sqlCommand = "SELECT project_role FROM project_members WHERE project_id = :projectId AND user_id = :userId";
        var params = buildParamsForProjectAndUser(projectId, userId);
        var projectRoleList = jdbcNativeTemplate.query(sqlCommand, params, (resultSet, rowNumber) -> ProjectRole.valueOf(resultSet.getString("project_role")));
        return projectRoleList.stream().findFirst();
    }

    @Override
    public void updateProjectRoleByProjectIdAndUserId(String projectId, String userId, ProjectRole projectRole) {
        var sqlCommand = buildSqlCommandForProjectRoleUpdate();
        var params = buildParamsForProjectAndUser(projectId, userId).addValue("projectRole", projectRole.name());
        var updatedRows = jdbcNativeTemplate.update(sqlCommand, params);

        if (updatedRows == 0) {
            throw new HubException("NOT_FOUND", "error.notFound", 404, userId);
        }
    }

    @Override
    public void removeProjectMemberByProjectIdAndUserId(String projectId, String userId) {
        var sqlCommand = buildSqlCommandForProjectMemberRemoval();
        var params = buildParamsForProjectAndUser(projectId, userId);
        var deletedRows = jdbcNativeTemplate.update(sqlCommand, params);

        if (deletedRows == 0) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }
    }

    private String buildSqlCommandForOwnerMember() {
        return new StringBuilder()
                .append("INSERT INTO project_members (project_id, user_id, project_role) ")
                .append("VALUES (:projectId, :userId, 'OWNER') ")
                .append("ON CONFLICT (project_id, user_id) DO UPDATE SET project_role = 'OWNER'")
                .toString();
    }

    private String buildSqlCommandForProjectMembersByProjectId() {
        return new StringBuilder()
                .append("SELECT member.project_id, member.user_id, member.project_role, member.created_at, ")
                .append("app_user.full_name, app_user.email ")
                .append("FROM project_members member ")
                .append("JOIN users app_user ON app_user.id = member.user_id ")
                .append("WHERE member.project_id = :projectId ")
                .append("ORDER BY member.created_at, member.user_id")
                .toString();
    }

    private String buildSqlCommandForProjectRoleUpdate() {
        return new StringBuilder()
                .append("UPDATE project_members ")
                .append("SET project_role = :projectRole ")
                .append("WHERE project_id = :projectId AND user_id = :userId ")
                .append("AND project_role <> 'OWNER'")
                .toString();
    }

    private String buildSqlCommandForProjectMemberRemoval() {
        return new StringBuilder()
                .append("DELETE FROM project_members ")
                .append("WHERE project_id = :projectId AND user_id = :userId ")
                .append("AND project_role <> 'OWNER'")
                .toString();
    }

    private MapSqlParameterSource buildParamsForProjectAndUser(String projectId, String userId) {
        return new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("userId", userId);
    }
}
