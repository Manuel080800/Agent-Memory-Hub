package io.agentmemoryhub.webapp.controller.project;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.McpConnectionInputRecord;
import io.agentmemoryhub.core.model.record.McpConnectionPageRecord;
import io.agentmemoryhub.core.model.record.McpConnectionRecord;
import io.agentmemoryhub.core.service.connection.McpConnectionService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/mcp-connections")
public class McpConnectionController {
    private final McpConnectionService mcpConnectionService;

    @GetMapping
    public ResponseEntity<McpConnectionPageRecord> listConnections(@PathVariable String projectId) {
        return ResponseEntity.ok(mcpConnectionService.listMcpConnectionsByProjectId(projectId));
    }

    @PostMapping
    public ResponseEntity<McpConnectionRecord> createConnection(@PathVariable String projectId, @RequestBody McpConnectionInputRecord mcpConnectionInputRecord) {
        return ResponseEntity.ok(mcpConnectionService.createMcpConnectionByProjectIdAndInput(projectId, mcpConnectionInputRecord));
    }

    @PutMapping("/{connectionId}")
    public ResponseEntity<McpConnectionRecord> updateConnection(@PathVariable String projectId, @PathVariable String connectionId, @RequestBody McpConnectionInputRecord mcpConnectionInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        validateConnectionBelongsToProject(projectId, connectionId);
        return ResponseEntity.ok(mcpConnectionService.updateMcpConnectionById(connectionId, mcpConnectionInputRecord, expectedVersion));
    }

    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> deleteConnection(@PathVariable String projectId, @PathVariable String connectionId, @RequestParam(defaultValue = "false") boolean confirm) {
        validateConnectionBelongsToProject(projectId, connectionId);
        mcpConnectionService.deleteMcpConnectionById(connectionId, confirm);
        return ResponseEntity.noContent().build();
    }

    private void validateConnectionBelongsToProject(String projectId, String connectionId) {
        var connectionIsInProject = mcpConnectionService.listMcpConnectionsByProjectId(projectId).items().stream().anyMatch(connectionRecord -> Objects.equals(connectionRecord.id(), connectionId));

        if (!connectionIsInProject) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, connectionId);
        }
    }
}
