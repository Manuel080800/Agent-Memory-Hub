package io.agentmemoryhub.core.dao;

import io.agentmemoryhub.core.model.record.IdempotencyKeyRecord;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IdempotencyKeyDAOImpl implements IdempotencyKeyDAO {
    private final NamedParameterJdbcTemplate jdbcNativeTemplate;

    @Override
    public int deleteExpiredIdempotencyKeyByProjectIdAndOperationAndRequestId(String projectId, String operation, String requestId) {
        var sqlCommand = buildSqlCommandForExpiredRequestDeletion();
        var params = buildParamsForRequest(projectId, operation, requestId);
        return jdbcNativeTemplate.update(sqlCommand, params);
    }

    @Override
    public int insertIdempotencyKeyIfAbsentByProjectIdAndOperationAndRequestId(String projectId, String operation, String requestId, String contentHash, String resourceId) {
        var sqlCommand = buildSqlCommandForResourceReservation();
        var params = buildParamsForResourceReservation(projectId, operation, requestId, contentHash, resourceId);
        return jdbcNativeTemplate.update(sqlCommand, params);
    }

    @Override
    public Optional<IdempotencyKeyRecord> findIdempotencyKeyByProjectIdAndOperationAndRequestId(String projectId, String operation, String requestId) {
        var sqlCommand = buildSqlCommandForRequestSelection();
        var params = buildParamsForRequest(projectId, operation, requestId);
        var idempotencyKeyRecordList = jdbcNativeTemplate.query(sqlCommand, params, (resultSet, rowNumber) -> new IdempotencyKeyRecord(resultSet.getString("content_hash"), resultSet.getString("resource_id")));
        return idempotencyKeyRecordList.stream().findFirst();
    }

    private String buildSqlCommandForExpiredRequestDeletion() {
        return new StringBuilder()
                .append("DELETE FROM idempotency_keys ")
                .append("WHERE project_id = :projectId AND operation = :operation ")
                .append("AND request_id = :requestId ")
                .append("AND created_at < now() - interval '7 days'")
                .toString();
    }

    private String buildSqlCommandForResourceReservation() {
        return new StringBuilder()
                .append("INSERT INTO idempotency_keys ")
                .append("(project_id, operation, request_id, content_hash, resource_id) ")
                .append("VALUES (:projectId, :operation, :requestId, :contentHash, :resourceId) ")
                .append("ON CONFLICT (project_id, operation, request_id) DO NOTHING")
                .toString();
    }

    private String buildSqlCommandForRequestSelection() {
        return new StringBuilder()
                .append("SELECT content_hash, resource_id ")
                .append("FROM idempotency_keys ")
                .append("WHERE project_id = :projectId ")
                .append("AND operation = :operation AND request_id = :requestId")
                .toString();
    }

    private MapSqlParameterSource buildParamsForRequest(String projectId, String operation, String requestId) {
        return new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("operation", operation)
                .addValue("requestId", requestId);
    }

    private MapSqlParameterSource buildParamsForResourceReservation(String projectId, String operation, String requestId, String contentHash, String resourceId) {
        return buildParamsForRequest(projectId, operation, requestId)
                .addValue("contentHash", contentHash)
                .addValue("resourceId", resourceId);
    }
}
