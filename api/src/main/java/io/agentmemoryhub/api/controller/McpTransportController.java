package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.api.common.McpEndpointType;
import io.agentmemoryhub.api.model.McpJsonRpcErrorRecord;
import io.agentmemoryhub.api.model.McpJsonRpcRequestRecord;
import io.agentmemoryhub.api.model.McpJsonRpcResponseRecord;
import io.agentmemoryhub.api.service.McpProtocolService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private static final String MODERN_MCP_PROTOCOL_VERSION = "2026-07-28";
    private static final String CURRENT_MCP_PROTOCOL_VERSION = "2025-11-25";
    private static final String LEGACY_MCP_PROTOCOL_VERSION = "2025-03-26";
    private static final List<String> SUPPORTED_MCP_PROTOCOL_VERSIONS = List.of("2026-07-28", "2025-11-25", "2025-06-18", "2025-03-26");
    private static final Set<String> SUPPORTED_LEGACY_MCP_PROTOCOL_VERSIONS = Set.of("2025-11-25", "2025-06-18", "2025-03-26");

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
        if (isModernRequest(jsonRpcRequestRecord, httpServletRequest)) {
            return createModernResponse(mcpEndpointType, jsonRpcRequestRecord, httpServletRequest, locale);
        }

        var responseProtocolVersion = resolveProtocolVersionByRequestAndHeader(jsonRpcRequestRecord, httpServletRequest.getHeader(MCP_PROTOCOL_VERSION_HEADER));

        if (Objects.isNull(responseProtocolVersion)) {
            return ResponseEntity.badRequest().build();
        }

        var responseRecord = mcpProtocolService.handleRequestByEndpointAndRequest(mcpEndpointType, jsonRpcRequestRecord, locale, responseProtocolVersion);

        if (responseRecord.isEmpty()) {
            return ResponseEntity.accepted().header(MCP_PROTOCOL_VERSION_HEADER, responseProtocolVersion).build();
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).header(MCP_PROTOCOL_VERSION_HEADER, responseProtocolVersion).body(responseRecord.get());
    }

    private ResponseEntity<?> createModernResponse(McpEndpointType mcpEndpointType, McpJsonRpcRequestRecord jsonRpcRequestRecord, HttpServletRequest httpServletRequest, Locale locale) {
        var headerProtocolVersion = httpServletRequest.getHeader(MCP_PROTOCOL_VERSION_HEADER);
        var bodyProtocolVersion = readModernProtocolVersion(jsonRpcRequestRecord);

        if (Objects.isNull(headerProtocolVersion) || headerProtocolVersion.isBlank() || Objects.isNull(bodyProtocolVersion) || !headerProtocolVersion.equals(bodyProtocolVersion)) {
            return createModernErrorResponse(jsonRpcRequestRecord, HttpStatus.BAD_REQUEST, -32020, "Header mismatch", null);
        }

        if (!MODERN_MCP_PROTOCOL_VERSION.equals(bodyProtocolVersion)) {
            return createModernErrorResponse(jsonRpcRequestRecord, HttpStatus.BAD_REQUEST, -32022, "Unsupported protocol version", Map.of("supported", SUPPORTED_MCP_PROTOCOL_VERSIONS, "requested", bodyProtocolVersion));
        }

        if (!hasValidModernMetadata(jsonRpcRequestRecord) || !hasValidModernHeaders(jsonRpcRequestRecord, httpServletRequest)) {
            return createModernErrorResponse(jsonRpcRequestRecord, HttpStatus.BAD_REQUEST, -32020, "Header mismatch", null);
        }

        if (Objects.isNull(jsonRpcRequestRecord) || Objects.isNull(jsonRpcRequestRecord.id())) {
            return createModernErrorResponse(jsonRpcRequestRecord, HttpStatus.BAD_REQUEST, -32600, "Invalid Request", null);
        }

        var responseRecord = mcpProtocolService.handleRequestByEndpointAndRequest(mcpEndpointType, jsonRpcRequestRecord, locale, bodyProtocolVersion);

        if (responseRecord.isEmpty()) {
            return ResponseEntity.accepted().build();
        }

        var responseStatus = Objects.nonNull(responseRecord.get().error()) && responseRecord.get().error().code() == -32601 ? HttpStatus.NOT_FOUND : HttpStatus.OK;
        return ResponseEntity.status(responseStatus).contentType(MediaType.APPLICATION_JSON).header(MCP_PROTOCOL_VERSION_HEADER, bodyProtocolVersion).body(responseRecord.get());
    }

    private ResponseEntity<?> createModernErrorResponse(McpJsonRpcRequestRecord jsonRpcRequestRecord, HttpStatus httpStatus, int errorCode, String message, Object errorData) {
        var responseRecord = new McpJsonRpcResponseRecord("2.0", Objects.isNull(jsonRpcRequestRecord) ? null : jsonRpcRequestRecord.id(), null, new McpJsonRpcErrorRecord(errorCode, message, errorData));
        return ResponseEntity.status(httpStatus).contentType(MediaType.APPLICATION_JSON).body(responseRecord);
    }

    private boolean hasValidModernMetadata(McpJsonRpcRequestRecord jsonRpcRequestRecord) {
        return Objects.nonNull(jsonRpcRequestRecord) && Objects.nonNull(jsonRpcRequestRecord.params()) && jsonRpcRequestRecord.params().path("_meta").isObject() && jsonRpcRequestRecord.params().path("_meta").path("io.modelcontextprotocol/clientCapabilities").isObject();
    }

    private boolean isModernRequest(McpJsonRpcRequestRecord jsonRpcRequestRecord, HttpServletRequest httpServletRequest) {
        var requestProtocolVersion = httpServletRequest.getHeader(MCP_PROTOCOL_VERSION_HEADER);
        return MODERN_MCP_PROTOCOL_VERSION.equals(requestProtocolVersion) || Objects.nonNull(readModernProtocolVersion(jsonRpcRequestRecord));
    }

    private String readModernProtocolVersion(McpJsonRpcRequestRecord jsonRpcRequestRecord) {
        if (Objects.isNull(jsonRpcRequestRecord) || Objects.isNull(jsonRpcRequestRecord.params())) {
            return null;
        }

        var versionNode = jsonRpcRequestRecord.params().path("_meta").path("io.modelcontextprotocol/protocolVersion");
        return versionNode.isTextual() ? versionNode.asText() : null;
    }

    private boolean hasValidModernHeaders(McpJsonRpcRequestRecord jsonRpcRequestRecord, HttpServletRequest httpServletRequest) {
        if (Objects.isNull(jsonRpcRequestRecord) || Objects.isNull(jsonRpcRequestRecord.method()) || !jsonRpcRequestRecord.method().equals(httpServletRequest.getHeader("Mcp-Method"))) {
            return false;
        }

        if ("tools/call".equals(jsonRpcRequestRecord.method())) {
            var requestToolName = Objects.isNull(jsonRpcRequestRecord.params()) ? null : jsonRpcRequestRecord.params().path("name").asText(null);
            var headerToolName = decodeHeaderValue(httpServletRequest.getHeader("Mcp-Name"));
            return Objects.nonNull(requestToolName) && requestToolName.equals(headerToolName);
        }

        return true;
    }

    private String decodeHeaderValue(String headerValue) {
        if (Objects.isNull(headerValue) || !headerValue.startsWith("=?base64?") || !headerValue.endsWith("?=")) {
            return headerValue;
        }

        try {
            var encodedValue = headerValue.substring(9, headerValue.length() - 2);
            return new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String resolveProtocolVersionByRequestAndHeader(McpJsonRpcRequestRecord jsonRpcRequestRecord, String requestProtocolVersion) {
        if (Objects.nonNull(jsonRpcRequestRecord) && "initialize".equals(jsonRpcRequestRecord.method())) {
            var requestedVersion = Objects.isNull(jsonRpcRequestRecord.params()) ? "" : jsonRpcRequestRecord.params().path("protocolVersion").asText("");
            if (SUPPORTED_LEGACY_MCP_PROTOCOL_VERSIONS.contains(requestedVersion)) {
                return requestedVersion;
            }
            return CURRENT_MCP_PROTOCOL_VERSION;
        }

        if (Objects.isNull(requestProtocolVersion) || requestProtocolVersion.isBlank()) {
            return LEGACY_MCP_PROTOCOL_VERSION;
        }

        if (SUPPORTED_LEGACY_MCP_PROTOCOL_VERSIONS.contains(requestProtocolVersion)) {
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

        if (Objects.isNull(originHeader) || originHeader.isBlank()) {
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
        if (Objects.isNull(hostHeader) || hostHeader.isBlank()) {
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
