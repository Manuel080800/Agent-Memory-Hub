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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        var request = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(1), "initialize", objectMapper.valueToTree(Map.of("protocolVersion", "2025-03-26")));

        var response = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, request, Locale.ENGLISH).orElseThrow();
        var result = (Map<?, ?>) objectMapper.convertValue(response.result(), Map.class);

        assertEquals("2025-03-26", result.get("protocolVersion"));
        var serverInfo = (Map<?, ?>) objectMapper.convertValue(result.get("serverInfo"), Map.class);
        assertEquals("agent-memory-hub-context", serverInfo.get("name"));
    }

    @Test
    void returnsProtocolErrorForUnknownToolsAndMethods() {
        // Verifies unknown methods and tool names use JSON-RPC protocol errors.
        when(mcpToolRegistry.listToolsByEndpoint(McpEndpointType.TASKS, Locale.ENGLISH)).thenReturn(List.of());
        var unknownToolRequest = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(2), "tools/call", objectMapper.valueToTree(Map.of("name", "missing", "arguments", Map.of())));
        var unknownMethodRequest = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(3), "missing/method", objectMapper.createObjectNode());

        var unknownToolResponse = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.TASKS, unknownToolRequest, Locale.ENGLISH).orElseThrow();
        var unknownMethodResponse = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.TASKS, unknownMethodRequest, Locale.ENGLISH).orElseThrow();

        assertEquals(-32602, unknownToolResponse.error().code());
        assertEquals(-32601, unknownMethodResponse.error().code());
        verify(mcpToolRegistry).listToolsByEndpoint(McpEndpointType.TASKS, Locale.ENGLISH);
    }

    @Test
    void acceptsNotificationsWithoutReturningJsonRpcResponses() {
        // Verifies valid notifications are acknowledged without a JSON-RPC response body.
        var request = new McpJsonRpcRequestRecord("2.0", null, "notifications/initialized", objectMapper.createObjectNode());

        var response = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, request, Locale.ENGLISH);

        assertTrue(response.isEmpty());
    }

    @Test
    void rejectsNonObjectRequestParameters() {
        // Verifies invalid JSON-RPC params receive an invalid-parameters error.
        var request = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(4), "ping", objectMapper.valueToTree("invalid"));

        var response = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, request, Locale.ENGLISH).orElseThrow();

        assertEquals(-32602, response.error().code());
        assertFalse(response.result() != null);
    }

    @Test
    void rejectsInitializeRequestsWithoutParametersAsInvalidParameters() {
        // Verifies malformed initialization gets a protocol parameter error instead of an internal error.
        var request = new McpJsonRpcRequestRecord("2.0", objectMapper.valueToTree(5), "initialize", null);

        var response = mcpProtocolService.handleRequestByEndpointAndRequest(McpEndpointType.CONTEXT, request, Locale.ENGLISH).orElseThrow();

        assertEquals(-32602, response.error().code());
    }
}
