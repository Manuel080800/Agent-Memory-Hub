package io.agentmemoryhub.core.service.idempotency;

public interface IdempotencyKeyService {
    String reserveResourceIdByRequest(String projectId, String operation, String requestId, String requestContent, String newResourceId);
}
