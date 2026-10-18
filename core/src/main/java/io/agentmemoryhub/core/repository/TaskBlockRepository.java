package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.model.domain.TaskBlock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskBlockRepository extends JpaRepository<TaskBlock, String> {
    List<TaskBlock> findByTaskIdOrderByCreatedAtAscIdAsc(String taskId);

    Optional<TaskBlock> findByIdAndTaskId(String id, String taskId);

    long countByTaskId(String taskId);
}
