package io.agentmemoryhub.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.api.common.McpEndpointType;
import io.agentmemoryhub.api.model.McpJsonRpcRequestRecord;
import io.agentmemoryhub.api.model.McpTextContentRecord;
import io.agentmemoryhub.api.model.McpToolDefinitionRecord;
import io.agentmemoryhub.api.model.McpToolResultRecord;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class McpProtocolServiceImplTest {

    private McpToolRegistry mcpToolRegistry;
    private McpProtocolServiceImpl mcpProtocolService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mcpToolRegistry = mock(McpToolRegistry.class);
        mcpProtocolService = new McpProtocolServiceImpl(mcpToolRegistry, new StaticMessageSource());
        objectMapper = new ObjectMapper();
    }

    @Test
    void negotiatesAProtocolVersionAndReturnsEndpointSpecificIdentity() {
        // Verifies initialization selects a supported client protocol version and server identity.
        var request = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(1), "initialize", objectMapper.valueToTree(Map.of("protocolVersion", "2025-11-25")));

        var response = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, request, Locale.ENGLISH, "2025-11-25").orElseThrow();
        var result = (Map<?, ?>) objectMapper.convertValue(response.result(), Map.class);

        assertEquals("2025-11-25", result.get("protocolVersion"));
        var serverInfo = (Map<?, ?>) objectMapper.convertValue(result.get("serverInfo"), Map.class);
        assertEquals("agent-memory-hub-context", serverInfo.get("name"));
    }

    @Test
    void returnsProtocolErrorForUnknownToolsAndMethods() {
        // Verifies unknown methods and tool names use JSON-RPC protocol errors.
        when(mcpToolRegistry.listToolsByEndpoint(McpEndpointType.TASKS, Locale.ENGLISH)).thenReturn(List.of());
        var unknownToolRequest = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(2), "tools/call", objectMapper.valueToTree(Map.of("name", "missing", "arguments", Map.of())));
        var unknownMethodRequest = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(3), "missing/method", objectMapper.createObjectNode());

        var unknownToolResponse = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.TASKS, unknownToolRequest, Locale.ENGLISH, "2025-11-25").orElseThrow();
        var unknownMethodResponse = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.TASKS, unknownMethodRequest, Locale.ENGLISH, "2025-11-25").orElseThrow();

        assertEquals(-32602, unknownToolResponse.error().code());
        assertEquals(-32601, unknownMethodResponse.error().code());
        verify(mcpToolRegistry).listToolsByEndpoint(McpEndpointType.TASKS, Locale.ENGLISH);
    }

    @Test
    void acceptsNotificationsWithoutReturningJsonRpcResponses() {
        // Verifies valid notifications are acknowledged without a JSON-RPC response body.
        var request = new McpJsonRpcRequestRecord("2.0", null, "notifications/initialized", objectMapper.createObjectNode());

        var response = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, request, Locale.ENGLISH, "2025-11-25");

        assertTrue(response.isEmpty());
    }

    @Test
    void rejectsNonObjectRequestParameters() {
        // Verifies invalid JSON-RPC params receive an invalid-parameters error.
        var request = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(4), "ping", objectMapper.valueToTree("invalid"));

        var response = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, request, Locale.ENGLISH, "2025-11-25").orElseThrow();

        assertEquals(-32602, response.error().code());
        assertTrue(Objects.isNull(response.result()));
    }

    @Test
    void rejectsInitializeRequestsWithoutParametersAsInvalidParameters() {
        // Verifies malformed initialization gets a protocol parameter error instead of an internal error.
        var request = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(5), "initialize", null);

        var response = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, request, Locale.ENGLISH, "2025-11-25").orElseThrow();

        assertEquals(-32602, response.error().code());
    }

    @Test
    void omitsStructuredOutputFieldsForProtocolVersionsThatDoNotSupportThem() {
        // Verifies 2025-03 clients receive valid legacy tool definitions and results without structured output fields.
        var toolDefinitionRecord = new McpToolDefinitionRecord("example_tool", "Example", Map.of("type", "object"), Map.of("type", "object"), Map.of("readOnlyHint", true));
        var toolResultRecord = new McpToolResultRecord(List.of(new McpTextContentRecord("text", "{}")), false, Map.of("success", true));
        when(mcpToolRegistry.listToolsByEndpoint(McpEndpointType.CONTEXT, Locale.ENGLISH)).thenReturn(List.of(toolDefinitionRecord));
        when(mcpToolRegistry.callToolByEndpointAndNameAndArguments(McpEndpointType.CONTEXT, "example_tool", objectMapper.createObjectNode(), Locale.ENGLISH)).thenReturn(toolResultRecord);
        var toolsListRequest = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(6), "tools/list", objectMapper.createObjectNode());
        var toolsCallRequest = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(7), "tools/call", objectMapper.valueToTree(Map.of("name", "example_tool", "arguments", Map.of())));

        var legacyToolsResponse = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, toolsListRequest, Locale.ENGLISH, "2025-03-26").orElseThrow();
        var legacyToolsResult = (Map<?, ?>) objectMapper.convertValue(legacyToolsResponse.result(), Map.class);
        var legacyToolDefinition = (Map<?, ?>) ((List<?>) legacyToolsResult.get("tools")).getFirst();
        var legacyCallResponse = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, toolsCallRequest, Locale.ENGLISH, "2025-03-26").orElseThrow();
        var legacyCallResult = (Map<?, ?>) objectMapper.convertValue(legacyCallResponse.result(), Map.class);

        assertFalse(legacyToolDefinition.containsKey("outputSchema"));
        assertFalse(legacyCallResult.containsKey("structuredContent"));
    }

    @Test
    void modernProtocolDiscoversVersionsAndReturnsModernToolResults() {
        // Verifies modern MCP discovery and tool responses include versioned result metadata and server identity.
        var toolDefinitionRecord = new McpToolDefinitionRecord("example_tool", "Example", Map.of("type", "object"), Map.of("type", "object"), Map.of("readOnlyHint", true));
        var toolResultRecord = new McpToolResultRecord(List.of(new McpTextContentRecord("text", "{}")), false, Map.of("success", true));
        when(mcpToolRegistry.listToolsByEndpoint(McpEndpointType.CONTEXT, Locale.ENGLISH)).thenReturn(List.of(toolDefinitionRecord));
        when(mcpToolRegistry.callToolByEndpointAndNameAndArguments(McpEndpointType.CONTEXT, "example_tool", objectMapper.createObjectNode(), Locale.ENGLISH)).thenReturn(toolResultRecord);
        var discoverRequest = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(8), "server/discover", objectMapper.createObjectNode());
        var toolsListRequest = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(9), "tools/list", objectMapper.createObjectNode());
        var toolsCallRequest = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(10), "tools/call", objectMapper.valueToTree(Map.of("name", "example_tool", "arguments", Map.of())));

        var discoverResponse = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, discoverRequest, Locale.ENGLISH, "2026-07-28").orElseThrow();
        var discoverResult = (Map<?, ?>) objectMapper.convertValue(discoverResponse.result(), Map.class);
        var toolsListResponse = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, toolsListRequest, Locale.ENGLISH, "2026-07-28").orElseThrow();
        var toolsListResult = (Map<?, ?>) objectMapper.convertValue(toolsListResponse.result(), Map.class);
        var modernToolDefinition = (Map<?, ?>) ((List<?>) toolsListResult.get("tools")).getFirst();
        var toolsCallResponse = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, toolsCallRequest, Locale.ENGLISH, "2026-07-28").orElseThrow();
        var toolsCallResult = (Map<?, ?>) objectMapper.convertValue(toolsCallResponse.result(), Map.class);

        assertEquals("complete", discoverResult.get("resultType"));
        assertTrue(((List<?>) discoverResult.get("supportedVersions")).contains("2026-07-28"));
        assertTrue(discoverResult.containsKey("_meta"));
        assertEquals("complete", toolsListResult.get("resultType"));
        assertTrue(modernToolDefinition.containsKey("outputSchema"));
        assertEquals("complete", toolsCallResult.get("resultType"));
        assertEquals(Map.of("success", true), toolsCallResult.get("structuredContent"));
        assertTrue(toolsCallResult.containsKey("_meta"));
    }
}
