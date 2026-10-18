package io.agentmemoryhub.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpJsonRpcErrorRecord(
        int code,
        String message,
        Object data
) {}
