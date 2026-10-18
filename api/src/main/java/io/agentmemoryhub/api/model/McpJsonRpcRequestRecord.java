package io.agentmemoryhub.api.model;

import com.fasterxml.jackson.databind.JsonNode;

public record McpJsonRpcRequestRecord(
        String jsonrpc,
        JsonNode id,
        String method,
        JsonNode params
) {}
