package io.agentmemoryhub.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpJsonRpcResponseRecord(
        String jsonrpc,
        JsonNode id,
        Object result,
        McpJsonRpcErrorRecord error
) {}
