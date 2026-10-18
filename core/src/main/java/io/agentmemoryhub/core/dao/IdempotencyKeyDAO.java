package io.agentmemoryhub.core.dao;

import io.agentmemoryhub.core.model.record.IdempotencyKeyRecord;
import java.util.Optional;

public interface IdempotencyKeyDAO {
    int deleteExpiredIdempotencyKeyByProjectIdAndOperationAndRequestId(String projectId, String operation, String requestId);

    int insertIdempotencyKeyIfAbsentByProjectIdAndOperationAndRequestId(String projectId, String operation, String requestId, String contentHash, String resourceId);

    Optional<IdempotencyKeyRecord> findIdempotencyKeyByProjectIdAndOperationAndRequestId(String projectId, String operation, String requestId);
}
