package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.model.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findByOwnerUserIdOrderByCreatedAtDescIdAsc(String ownerUserId);

    Optional<Project> findByIdAndOwnerUserId(String id, String ownerUserId);
}
