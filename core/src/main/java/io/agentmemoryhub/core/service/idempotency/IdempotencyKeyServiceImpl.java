package io.agentmemoryhub.core.service.idempotency;

import io.agentmemoryhub.core.dao.IdempotencyKeyDAO;
import io.agentmemoryhub.core.exception.HubException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdempotencyKeyServiceImpl implements IdempotencyKeyService {
    private final IdempotencyKeyDAO idempotencyKeyDAO;

    @Override
    @Transactional
    public String reserveResourceIdByRequest(String projectId, String operation, String requestId, String requestContent, String newResourceId) {
        idempotencyKeyDAO.deleteExpiredIdempotencyKeyByProjectIdAndOperationAndRequestId(projectId, operation, requestId);

        var requestHash = hashContent(requestContent);
        idempotencyKeyDAO.insertIdempotencyKeyIfAbsentByProjectIdAndOperationAndRequestId(projectId, operation, requestId, requestHash, newResourceId);

        var idempotencyKeyRecord = idempotencyKeyDAO.findIdempotencyKeyByProjectIdAndOperationAndRequestId(projectId, operation, requestId).orElseThrow(() -> new HubException("CONNECTION_UNAVAILABLE", "error.connectionUnavailable", 503));

        if (!idempotencyKeyRecord.contentHash().equals(requestHash)) {
            throw new HubException("CONFLICT", "error.conflict", 409, operation);
        }

        return idempotencyKeyRecord.resourceId();
    }

    private String hashContent(String requestContent) {
        try {
            var messageDigest = MessageDigest.getInstance("SHA-256");
            var digest = messageDigest.digest(requestContent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available in this Java runtime.", exception);
        }
    }
}
