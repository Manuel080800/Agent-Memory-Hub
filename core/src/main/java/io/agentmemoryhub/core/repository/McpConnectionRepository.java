package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.common.ConnectionType;
import io.agentmemoryhub.core.model.domain.McpConnection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McpConnectionRepository extends JpaRepository<McpConnection, String> {
    List<McpConnection> findByProjectIdOrderByConnectionTypeAsc(String projectId);

    Optional<McpConnection> findByProjectIdAndConnectionType(String projectId, ConnectionType connectionType);
}
