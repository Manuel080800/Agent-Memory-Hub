package io.agentmemoryhub.api.service;

import io.agentmemoryhub.api.common.McpEndpointType;
import io.agentmemoryhub.api.model.McpJsonRpcErrorRecord;
import io.agentmemoryhub.api.model.McpJsonRpcRequestRecord;
import io.agentmemoryhub.api.model.McpJsonRpcResponseRecord;
import io.agentmemoryhub.api.model.McpToolResultRecord;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class McpProtocolServiceImpl implements McpProtocolService {
    private static final String JSON_RPC_VERSION = "2.0";
    private static final String MODERN_MCP_PROTOCOL_VERSION = "2026-07-28";
    private static final String MCP_PROTOCOL_VERSION = "2025-11-25";
    private static final Set<String> SUPPORTED_LEGACY_MCP_PROTOCOL_VERSIONS = Set.of("2025-11-25", "2025-06-18", "2025-03-26");
    private static final List<String> SUPPORTED_MCP_PROTOCOL_VERSIONS = List.of("2026-07-28", "2025-11-25", "2025-06-18", "2025-03-26");

    private final McpToolRegistry mcpToolRegistry;
    private final MessageSource messageSource;

    @Override
    public Optional<McpJsonRpcResponseRecord> handleRequestByEndpointAndRequest(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord, Locale locale, String protocolVersion) {
        if (Objects.isNull(jsonRpcRequestRecord) || !JSON_RPC_VERSION.equals(jsonRpcRequestRecord.jsonrpc()) || Objects.isNull(jsonRpcRequestRecord.method()) || jsonRpcRequestRecord.method().isBlank()) {
            return createErrorResponse(jsonRpcRequestRecord, -32600, "mcp.error.invalidRequest", locale);
        }

        if (Objects.isNull(jsonRpcRequestRecord.id())) {
            return Optional.empty();
        }

        if ("initialize".equals(jsonRpcRequestRecord.method()) && Objects.isNull(jsonRpcRequestRecord.params())) {
            return createErrorResponse(jsonRpcRequestRecord, -32602, "mcp.error.invalidParameters", locale);
        }

        if (Objects.nonNull(jsonRpcRequestRecord.params()) && !jsonRpcRequestRecord.params().isObject()) {
            return createErrorResponse(jsonRpcRequestRecord, -32602, "mcp.error.invalidParameters", locale);
        }

        try {
            return switch (jsonRpcRequestRecord.method()) {
                case "initialize" -> Optional.of(createInitializeResponse(mcpEndpointType, jsonRpcRequestRecord));
                case "server/discover" -> MODERN_MCP_PROTOCOL_VERSION.equals(protocolVersion) ? Optional.of(createDiscoverResponse(mcpEndpointType, jsonRpcRequestRecord)) : createErrorResponse(jsonRpcRequestRecord, -32601, "mcp.error.methodNotFound", locale);
                case "ping" -> Optional.of(createSuccessResponse(jsonRpcRequestRecord, MODERN_MCP_PROTOCOL_VERSION.equals(protocolVersion) ? Map.of("_meta", createServerMetadata(mcpEndpointType)) : Map.of()));
                case "tools/list" -> Optional.of(createToolsListResponse(mcpEndpointType, jsonRpcRequestRecord, locale, protocolVersion));
                case "tools/call" -> createToolCallResponse(mcpEndpointType, jsonRpcRequestRecord, locale, protocolVersion);
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
        var negotiatedProtocolVersion = SUPPORTED_LEGACY_MCP_PROTOCOL_VERSIONS.contains(requestedProtocolVersion) ? requestedProtocolVersion : MCP_PROTOCOL_VERSION;
        var result = Map.<String, Object>of("protocolVersion", negotiatedProtocolVersion, "capabilities", Map.of("tools", Map.of("listChanged", false)), "serverInfo", Map.of("name", serverName, "version", "1.0.0"));
        return createSuccessResponse(jsonRpcRequestRecord, result);
    }

    private McpJsonRpcResponseRecord createDiscoverResponse(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord) {
        var result = Map.<String, Object>of("resultType", "complete", "supportedVersions", SUPPORTED_MCP_PROTOCOL_VERSIONS, "capabilities", Map.of("tools", Map.of()), "_meta", createServerMetadata(mcpEndpointType));
        return createSuccessResponse(jsonRpcRequestRecord, result);
    }

    private McpJsonRpcResponseRecord createToolsListResponse(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord, Locale locale, String protocolVersion) {
        var toolDefinitionRecordList = mcpToolRegistry.listToolsByEndpoint(mcpEndpointType, locale);
        var toolDefinitionList = supportsStructuredToolOutput(protocolVersion) ? toolDefinitionRecordList : toolDefinitionRecordList.stream().map(toolDefinitionRecord -> Map.of("name", toolDefinitionRecord.name(), "description", toolDefinitionRecord.description(), "inputSchema", toolDefinitionRecord.inputSchema(), "annotations", toolDefinitionRecord.annotations())).toList();
        var result = MODERN_MCP_PROTOCOL_VERSION.equals(protocolVersion) ? Map.<String, Object>of("resultType", "complete", "tools", toolDefinitionList, "_meta", createServerMetadata(mcpEndpointType)) : Map.<String, Object>of("tools", toolDefinitionList);
        return createSuccessResponse(jsonRpcRequestRecord, result);
    }

    private Optional<McpJsonRpcResponseRecord> createToolCallResponse(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord, Locale locale, String protocolVersion) {
        if (Objects.isNull(jsonRpcRequestRecord.params()) || jsonRpcRequestRecord.params().path("name").isMissingNode() || !jsonRpcRequestRecord.params().path("name").isTextual()) {
            return createErrorResponse(jsonRpcRequestRecord, -32602, "mcp.error.toolNameRequired", locale);
        }

        var toolName = jsonRpcRequestRecord.params().path("name").asText();
        var toolExists = mcpToolRegistry.listToolsByEndpoint(mcpEndpointType, locale).stream().anyMatch(toolDefinitionRecord -> toolDefinitionRecord.name().equals(toolName));

        if (!toolExists) {
            return createErrorResponse(jsonRpcRequestRecord, -32602, "mcp.error.unknownTool", locale);
        }

        var toolResultRecord = mcpToolRegistry.callToolByEndpointAndNameAndArguments(mcpEndpointType, toolName, jsonRpcRequestRecord.params().path("arguments"), locale);
        var protocolToolResult = MODERN_MCP_PROTOCOL_VERSION.equals(protocolVersion) ? createModernToolResult(mcpEndpointType, toolResultRecord) : supportsStructuredToolOutput(protocolVersion) ? toolResultRecord : Map.of("content", toolResultRecord.content(), "isError", toolResultRecord.isError());
        return Optional.of(createSuccessResponse(jsonRpcRequestRecord, protocolToolResult));
    }

    private Map<String, Object> createModernToolResult(McpEndpointType mcpEndpointType, McpToolResultRecord toolResultRecord) {
        var protocolToolResult = new LinkedHashMap<String, Object>();
        protocolToolResult.put("resultType", "complete");
        protocolToolResult.put("content", toolResultRecord.content());
        protocolToolResult.put("isError", toolResultRecord.isError());
        protocolToolResult.put("_meta", createServerMetadata(mcpEndpointType));

        if (Objects.nonNull(toolResultRecord.structuredContent())) {
            protocolToolResult.put("structuredContent", toolResultRecord.structuredContent());
        }

        return protocolToolResult;
    }

    private Map<String, Object> createServerMetadata(McpEndpointType mcpEndpointType) {
        var serverName = mcpEndpointType == McpEndpointType.CONTEXT ? "agent-memory-hub-context" : "agent-memory-hub-tasks";
        return Map.of("io.modelcontextprotocol/serverInfo", Map.of("name", serverName, "version", "1.0.0"));
    }

    private boolean supportsStructuredToolOutput(String protocolVersion) {
        return MODERN_MCP_PROTOCOL_VERSION.equals(protocolVersion) || "2025-06-18".equals(protocolVersion) || "2025-11-25".equals(protocolVersion);
    }

    private McpJsonRpcResponseRecord createSuccessResponse(McpJsonRpcRequestRecord request, Object result) {
        return new McpJsonRpcResponseRecord(JSON_RPC_VERSION, request.id(), result, null);
    }

    private Optional<McpJsonRpcResponseRecord> createErrorResponse(McpJsonRpcRequestRecord request, int errorCode, String messageKey, Locale locale) {
        if (Objects.isNull(request) || Objects.isNull(request.id())) {
            return Optional.empty();
        }

        var errorMessage = messageSource.getMessage(messageKey, null, messageKey, locale);
        var jsonRpcErrorRecord = new McpJsonRpcErrorRecord(errorCode, errorMessage, null);
        return Optional.of(new McpJsonRpcResponseRecord(JSON_RPC_VERSION, request.id(), null, jsonRpcErrorRecord));
    }
}
