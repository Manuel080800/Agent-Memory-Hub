package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.api.common.McpEndpointType;
import io.agentmemoryhub.api.model.McpJsonRpcRequestRecord;
import io.agentmemoryhub.api.service.McpProtocolService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mcp")
public class McpTransportController {
    private static final String MCP_PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    private static final String CURRENT_MCP_PROTOCOL_VERSION = "2025-06-18";
    private static final String LEGACY_MCP_PROTOCOL_VERSION = "2025-03-26";

    private final McpProtocolService mcpProtocolService;

    @PostMapping(value = "/context", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleContextRequest(@RequestBody McpJsonRpcRequestRecord jsonRpcRequestRecord, HttpServletRequest httpServletRequest, Locale locale) {
        if (!isTrustedLocalRequest(httpServletRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return createResponse(McpEndpointType.CONTEXT, jsonRpcRequestRecord, httpServletRequest, locale);
    }

    @PostMapping(value = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleTaskRequest(@RequestBody McpJsonRpcRequestRecord jsonRpcRequestRecord, HttpServletRequest httpServletRequest, Locale locale) {
        if (!isTrustedLocalRequest(httpServletRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return createResponse(McpEndpointType.TASKS, jsonRpcRequestRecord, httpServletRequest, locale);
    }

    private ResponseEntity<?> createResponse(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord, HttpServletRequest httpServletRequest, Locale locale) {
        var responseProtocolVersion = resolveProtocolVersionByRequestAndHeader(jsonRpcRequestRecord, httpServletRequest.getHeader(MCP_PROTOCOL_VERSION_HEADER));

        if (Objects.isNull(responseProtocolVersion)) {
            return ResponseEntity.badRequest().build();
        }

        var responseRecord = mcpProtocolService.handleRequestByEndpointAndRequest(mcpEndpointType, jsonRpcRequestRecord, locale);

        if (responseRecord.isEmpty()) {
            return ResponseEntity.accepted().header(MCP_PROTOCOL_VERSION_HEADER, responseProtocolVersion).build();
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).header(MCP_PROTOCOL_VERSION_HEADER, responseProtocolVersion).body(responseRecord.get());
    }

    private String resolveProtocolVersionByRequestAndHeader(McpJsonRpcRequestRecord jsonRpcRequestRecord, String requestProtocolVersion) {
        if (jsonRpcRequestRecord != null && "initialize".equals(jsonRpcRequestRecord.method())) {
            var requestedVersion = jsonRpcRequestRecord.params() == null ? "" : jsonRpcRequestRecord.params().path("protocolVersion").asText("");
            if (CURRENT_MCP_PROTOCOL_VERSION.equals(requestedVersion) || LEGACY_MCP_PROTOCOL_VERSION.equals(requestedVersion)) {
                return requestedVersion;
            }
            return CURRENT_MCP_PROTOCOL_VERSION;
        }

        if (requestProtocolVersion == null || requestProtocolVersion.isBlank()) {
            return LEGACY_MCP_PROTOCOL_VERSION;
        }

        if (CURRENT_MCP_PROTOCOL_VERSION.equals(requestProtocolVersion) || LEGACY_MCP_PROTOCOL_VERSION.equals(requestProtocolVersion)) {
            return requestProtocolVersion;
        }

        return null;
    }

    private boolean isTrustedLocalRequest(HttpServletRequest httpServletRequest) {
        if (!httpServletRequest.isSecure()) {
            return false;
        }

        var hostHeader = httpServletRequest.getHeader(HttpHeaders.HOST);

        if (!isLoopbackHost(hostHeader)) {
            return false;
        }

        var originHeader = httpServletRequest.getHeader(HttpHeaders.ORIGIN);

        if (originHeader == null || originHeader.isBlank()) {
            return true;
        }

        try {
            var originUri = URI.create(originHeader);
            return "https".equalsIgnoreCase(originUri.getScheme()) && isLoopbackHost(originUri.getRawAuthority());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isLoopbackHost(String hostHeader) {
        if (hostHeader == null || hostHeader.isBlank()) {
            return false;
        }

        try {
            var hostUri = URI.create("https://" + hostHeader);
            var host = Optional.ofNullable(hostUri.getHost()).orElse("").toLowerCase(Locale.ROOT);
            return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1") || host.equals("[::1]");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
