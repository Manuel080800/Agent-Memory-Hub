package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.McpConnectionInputRecord;
import io.agentmemoryhub.core.model.record.McpConnectionPageRecord;
import io.agentmemoryhub.core.model.record.McpConnectionRecord;
import io.agentmemoryhub.core.service.connection.McpConnectionService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/mcp-connections")
public class McpConnectionController {
    private final McpConnectionService mcpConnectionService;

    @GetMapping
    public McpConnectionPageRecord listConnections(@PathVariable String projectId) {
        return mcpConnectionService.listMcpConnectionsByProjectId(projectId);
    }

    @PostMapping
    public McpConnectionRecord createConnection(@PathVariable String projectId, @RequestBody McpConnectionInputRecord inputRecord) {
        return mcpConnectionService.createMcpConnectionByProjectIdAndInput(projectId, inputRecord);
    }

    @PutMapping("/{connectionId}")
    public McpConnectionRecord updateConnection(@PathVariable String projectId, @PathVariable String connectionId, @RequestBody McpConnectionInputRecord inputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        validateConnectionBelongsToProject(projectId, connectionId);
        return mcpConnectionService.updateMcpConnectionById(connectionId, inputRecord, expectedVersion);
    }

    @DeleteMapping("/{connectionId}")
    public void deleteConnection(@PathVariable String projectId, @PathVariable String connectionId, @RequestParam(defaultValue = "false") boolean confirm) {
        validateConnectionBelongsToProject(projectId, connectionId);
        mcpConnectionService.deleteMcpConnectionById(connectionId, confirm);
    }

    private void validateConnectionBelongsToProject(String projectId, String connectionId) {
        var connectionIsInProject = mcpConnectionService.listMcpConnectionsByProjectId(projectId).items().stream().anyMatch(connectionRecord -> Objects.equals(connectionRecord.id(), connectionId));

        if (!connectionIsInProject) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, connectionId);
        }
    }
}
