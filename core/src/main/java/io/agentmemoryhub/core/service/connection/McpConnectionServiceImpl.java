package io.agentmemoryhub.core.service.connection;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.McpConnection;
import io.agentmemoryhub.core.model.record.McpConnectionInputRecord;
import io.agentmemoryhub.core.model.record.McpConnectionPageRecord;
import io.agentmemoryhub.core.model.record.McpConnectionRecord;
import io.agentmemoryhub.core.repository.McpConnectionRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class McpConnectionServiceImpl implements McpConnectionService {
    private final McpConnectionRepository mcpConnectionRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public McpConnectionPageRecord listMcpConnectionsByProjectId(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new HubException("NOT_FOUND", "error.notFound", 404, projectId);
        }

        var mcpConnectionEntityList = mcpConnectionRepository.findByProjectIdOrderByConnectionTypeAsc(projectId);
        var connectionRecordList = mcpConnectionEntityList.stream().map(this::toMcpConnectionRecord).toList();
        return new McpConnectionPageRecord(connectionRecordList, 0, connectionRecordList.size(), connectionRecordList.size());
    }

    @Override
    @Transactional
    public McpConnectionRecord createMcpConnectionByProjectIdAndInput(String projectId, McpConnectionInputRecord inputRecord) {
        validateMcpConnectionInput(inputRecord, true);

        if (!projectRepository.existsById(projectId)) {
            throw new HubException("NOT_FOUND", "error.notFound", 404, projectId);
        }

        var projectConnection = mcpConnectionRepository.findByProjectIdAndConnectionType(projectId, inputRecord.connectionType());

        if (projectConnection.isPresent()) {
            throw new HubException("CONFLICT", "error.conflict", 409, inputRecord.connectionType());
        }

        var mcpConnection = new McpConnection();
        mcpConnection.setProjectId(projectId);
        mcpConnection.setName(inputRecord.name().strip());
        mcpConnection.setConnectionType(inputRecord.connectionType());
        mcpConnection.setEnabled(Objects.isNull(inputRecord.enabled()) || inputRecord.enabled());
        return toMcpConnectionRecord(mcpConnectionRepository.save(mcpConnection));
    }

    @Override
    @Transactional
    public McpConnectionRecord updateMcpConnectionById(String connectionId, McpConnectionInputRecord inputRecord, Long expectedVersion) {
        validateMcpConnectionInput(inputRecord, false);
        var mcpConnection = findMcpConnectionById(connectionId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion != mcpConnection.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, connectionId);
        }

        if (Objects.nonNull(inputRecord.connectionType())) {
            if (inputRecord.connectionType() != mcpConnection.getConnectionType()) {
                throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
            }
        }

        if (Objects.nonNull(inputRecord.name())) {
            mcpConnection.setName(inputRecord.name().strip());
        }

        if (Objects.nonNull(inputRecord.enabled())) {
            mcpConnection.setEnabled(inputRecord.enabled());
        }

        return toMcpConnectionRecord(mcpConnectionRepository.save(mcpConnection));
    }

    @Override
    @Transactional
    public void deleteMcpConnectionById(String connectionId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        mcpConnectionRepository.delete(findMcpConnectionById(connectionId));
    }

    private void validateMcpConnectionInput(McpConnectionInputRecord inputRecord, boolean create) {
        if (Objects.isNull(inputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "connection");
        }

        if (create && hasInvalidCreateFields(inputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "connection");
        }

        if (!create && hasNoUpdateFields(inputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "connection");
        }
    }

    private McpConnection findMcpConnectionById(String connectionId) {
        return mcpConnectionRepository.findById(connectionId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, connectionId));
    }

    private boolean hasInvalidCreateFields(McpConnectionInputRecord inputRecord) {
        return ValidateUtils.isInvalidString(inputRecord.name()) || Objects.isNull(inputRecord.connectionType());
    }

    private boolean hasNoUpdateFields(McpConnectionInputRecord inputRecord) {
        return Objects.isNull(inputRecord.name()) && Objects.isNull(inputRecord.enabled()) && Objects.isNull(inputRecord.connectionType());
    }

    private McpConnectionRecord toMcpConnectionRecord(McpConnection mcpConnection) {
        return new McpConnectionRecord(mcpConnection.getId(), mcpConnection.getProjectId(), mcpConnection.getName(), mcpConnection.getConnectionType(), mcpConnection.isEnabled(), mcpConnection.getCreatedAt(), mcpConnection.getUpdatedAt(), mcpConnection.getVersion());
    }
}
