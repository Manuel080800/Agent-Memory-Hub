package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.model.domain.Task;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByProfileIdOrderByCreatedAtDescIdAsc(String profileId);

    Optional<Task> findByIdAndProfileId(String id, String profileId);
}
