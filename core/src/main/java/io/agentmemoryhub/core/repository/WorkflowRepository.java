package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.model.domain.Workflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<Workflow, String> {
    List<Workflow> findByProfileIdOrderByCreatedAtAscIdAsc(String profileId);

    List<Workflow> findByProfileIdAndEnabledTrueOrderByCreatedAtAscIdAsc(String profileId);

    Optional<Workflow> findByIdAndProfileId(String id, String profileId);

    Optional<Workflow> findByIdAndProfileIdAndEnabledTrue(String id, String profileId);
}
