package io.agentmemoryhub.api.service;

import io.agentmemoryhub.api.common.McpEndpointType;
import io.agentmemoryhub.api.model.McpJsonRpcErrorRecord;
import io.agentmemoryhub.api.model.McpJsonRpcRequestRecord;
import io.agentmemoryhub.api.model.McpJsonRpcResponseRecord;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class McpProtocolServiceImpl implements McpProtocolService {
    private static final String JSON_RPC_VERSION = "2.0";
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";
    private static final Set<String> SUPPORTED_MCP_PROTOCOL_VERSIONS = Set.of("2025-06-18", "2025-03-26");

    private final McpToolRegistry mcpToolRegistry;
    private final MessageSource messageSource;

    @Override
    public Optional<McpJsonRpcResponseRecord> handleRequestByEndpointAndRequest(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord, Locale locale) {
        if (jsonRpcRequestRecord == null || !JSON_RPC_VERSION.equals(jsonRpcRequestRecord.jsonrpc()) || jsonRpcRequestRecord.method() == null || jsonRpcRequestRecord.method().isBlank()) {
            return createErrorResponse(jsonRpcRequestRecord, -32600, "mcp.error.invalidRequest", locale);
        }

        if (jsonRpcRequestRecord.id() == null) {
            return Optional.empty();
        }

        if ("initialize".equals(jsonRpcRequestRecord.method()) && jsonRpcRequestRecord.params() == null) {
            return createErrorResponse(jsonRpcRequestRecord, -32602, "mcp.error.invalidParameters", locale);
        }

        if (jsonRpcRequestRecord.params() != null && !jsonRpcRequestRecord.params().isObject()) {
            return createErrorResponse(jsonRpcRequestRecord, -32602, "mcp.error.invalidParameters", locale);
        }

        try {
            return switch (jsonRpcRequestRecord.method()) {
                case "initialize" -> Optional.of(createInitializeResponse(mcpEndpointType, jsonRpcRequestRecord));
                case "ping" -> Optional.of(createSuccessResponse(jsonRpcRequestRecord, Map.of()));
                case "tools/list" -> Optional.of(createToolsListResponse(mcpEndpointType, jsonRpcRequestRecord, locale));
                case "tools/call" -> createToolCallResponse(mcpEndpointType, jsonRpcRequestRecord, locale);
                case "notifications/initialized" -> Optional.empty();
                default -> createErrorResponse(jsonRpcRequestRecord, -32601, "mcp.error.methodNotFound", locale);
            };
        } catch (RuntimeException exception) {
            return createErrorResponse(jsonRpcRequestRecord, -32603, "mcp.error.internal", locale);
        }
    }

    private McpJsonRpcResponseRecord createInitializeResponse(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord) {
        var serverName = mcpEndpointType == McpEndpointType.CONTEXT ? "agent-memory-hub-context" : "agent-memory-hub-tasks";
        var requestedProtocolVersion = jsonRpcRequestRecord.params().path("protocolVersion").asText(MCP_PROTOCOL_VERSION);
        var negotiatedProtocolVersion = SUPPORTED_MCP_PROTOCOL_VERSIONS.contains(requestedProtocolVersion) ? requestedProtocolVersion : MCP_PROTOCOL_VERSION;
        var result = Map.<String, Object>of("protocolVersion", negotiatedProtocolVersion, "capabilities", Map.of("tools", Map.of("listChanged", false)), "serverInfo", Map.of("name", serverName, "version", "1.0.0"));
        return createSuccessResponse(jsonRpcRequestRecord, result);
    }

    private McpJsonRpcResponseRecord createToolsListResponse(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord, Locale locale) {
        var result = Map.<String, Object>of("tools", mcpToolRegistry.listToolsByEndpoint(mcpEndpointType, locale));
        return createSuccessResponse(jsonRpcRequestRecord, result);
    }

    private Optional<McpJsonRpcResponseRecord> createToolCallResponse(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord, Locale locale) {
        if (jsonRpcRequestRecord.params() == null || jsonRpcRequestRecord.params().path("name").isMissingNode() || !jsonRpcRequestRecord.params().path("name").isTextual()) {
            return createErrorResponse(jsonRpcRequestRecord, -32602, "mcp.error.toolNameRequired", locale);
        }

        var toolName = jsonRpcRequestRecord.params().path("name").asText();
        var toolExists = mcpToolRegistry.listToolsByEndpoint(mcpEndpointType, locale).stream().anyMatch(toolDefinitionRecord -> toolDefinitionRecord.name().equals(toolName));

        if (!toolExists) {
            return createErrorResponse(jsonRpcRequestRecord, -32602, "mcp.error.unknownTool", locale);
        }

        var toolResultRecord = mcpToolRegistry.callToolByEndpointAndNameAndArguments(mcpEndpointType, toolName, jsonRpcRequestRecord.params().path("arguments"), locale);
        return Optional.of(createSuccessResponse(jsonRpcRequestRecord, toolResultRecord));
    }

    private McpJsonRpcResponseRecord createSuccessResponse(McpJsonRpcRequestRecord request, Object result) {
        return new McpJsonRpcResponseRecord(JSON_RPC_VERSION, request.id(), result, null);
    }

    private Optional<McpJsonRpcResponseRecord> createErrorResponse(McpJsonRpcRequestRecord request, int errorCode, String messageKey, Locale locale) {
        if (request == null || request.id() == null) {
            return Optional.empty();
        }

        var errorMessage = messageSource.getMessage(messageKey, null, messageKey, locale);
        var jsonRpcErrorRecord = new McpJsonRpcErrorRecord(errorCode, errorMessage, null);
        return Optional.of(new McpJsonRpcResponseRecord(JSON_RPC_VERSION, request.id(), null, jsonRpcErrorRecord));
    }
}
