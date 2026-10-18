package io.agentmemoryhub.core.service.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.common.ConnectionType;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.McpConnection;
import io.agentmemoryhub.core.model.record.McpConnectionInputRecord;
import io.agentmemoryhub.core.repository.McpConnectionRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class McpConnectionServiceImplTest {
    @Mock
    private McpConnectionRepository mcpConnectionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private McpConnectionServiceImpl mcpConnectionService;

    @Test
    void createsConnectionWithTrimmedNameAndEnabledDefault() {
        // Verifies a project connection is created with normalized name and enabled defaults.
        when(projectRepository.existsById("project-1")).thenReturn(true);
        when(mcpConnectionRepository.findByProjectIdAndConnectionType("project-1", ConnectionType.CONTEXT)).thenReturn(Optional.empty());
        when(mcpConnectionRepository.save(any(McpConnection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var connectionRecord = mcpConnectionService.createMcpConnectionByProjectIdAndInput("project-1", new McpConnectionInputRecord("  Context  ", ConnectionType.CONTEXT, null));
        var connectionCaptor = ArgumentCaptor.forClass(McpConnection.class);

        org.mockito.Mockito.verify(mcpConnectionRepository).save(connectionCaptor.capture());
        assertEquals("Context", connectionRecord.name());
        assertEquals("project-1", connectionCaptor.getValue().getProjectId());
        org.junit.jupiter.api.Assertions.assertTrue(connectionRecord.enabled());
    }

    @Test
    void rejectsChangingConnectionTypeDuringUpdate() {
        // Verifies a connection cannot be switched to another endpoint type after creation.
        var connection = new McpConnection();
        connection.setConnectionType(ConnectionType.CONTEXT);
        when(mcpConnectionRepository.findById(connection.getId())).thenReturn(Optional.of(connection));

        var exception = assertThrows(HubException.class, () -> mcpConnectionService.updateMcpConnectionById(connection.getId(), new McpConnectionInputRecord(null, ConnectionType.TASKS, null), 0L));

        assertEquals("OPERATION_NOT_ALLOWED", exception.getCode());
    }
}
