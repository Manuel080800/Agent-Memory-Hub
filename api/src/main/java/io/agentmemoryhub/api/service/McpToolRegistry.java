package io.agentmemoryhub.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.agentmemoryhub.api.common.McpEndpointType;
import io.agentmemoryhub.api.model.McpToolDefinitionRecord;
import io.agentmemoryhub.api.model.McpToolResultRecord;
import java.util.Locale;
import java.util.List;

public interface McpToolRegistry {
    List<McpToolDefinitionRecord> listToolsByEndpoint(McpEndpointType mcpEndpointType, Locale locale);

    McpToolResultRecord callToolByEndpointAndNameAndArguments(McpEndpointType mcpEndpointType, String toolName, JsonNode arguments, Locale locale);
}
