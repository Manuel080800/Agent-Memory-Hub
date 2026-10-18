package io.agentmemoryhub.api.service;

import io.agentmemoryhub.api.common.McpEndpointType;
import io.agentmemoryhub.api.model.McpJsonRpcRequestRecord;
import io.agentmemoryhub.api.model.McpJsonRpcResponseRecord;
import java.util.Locale;
import java.util.Optional;

public interface McpProtocolService {
    Optional<McpJsonRpcResponseRecord> handleRequestByEndpointAndRequest(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord, Locale locale, String protocolVersion);
}
