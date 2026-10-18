package io.agentmemoryhub.core.service.connection;

import io.agentmemoryhub.core.model.record.McpConnectionInputRecord;
import io.agentmemoryhub.core.model.record.McpConnectionPageRecord;
import io.agentmemoryhub.core.model.record.McpConnectionRecord;

public interface McpConnectionService {
    McpConnectionPageRecord listMcpConnectionsByProjectId(String projectId);

    McpConnectionRecord createMcpConnectionByProjectIdAndInput(String projectId, McpConnectionInputRecord mcpConnectionInputRecord);

    McpConnectionRecord updateMcpConnectionById(String connectionId, McpConnectionInputRecord mcpConnectionInputRecord, Long expectedVersion);

    void deleteMcpConnectionById(String connectionId, boolean confirm);
}
