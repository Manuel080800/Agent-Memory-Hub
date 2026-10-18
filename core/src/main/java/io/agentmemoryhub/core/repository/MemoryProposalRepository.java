package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.model.domain.MemoryProposal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryProposalRepository extends JpaRepository<MemoryProposal, String> {
    List<MemoryProposal> findByTaskIdOrderByCreatedAtAscIdAsc(String taskId);

    Optional<MemoryProposal> findByIdAndTaskId(String id, String taskId);
}
