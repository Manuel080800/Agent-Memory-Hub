package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.model.domain.Memory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryRepository extends JpaRepository<Memory, String> {
    List<Memory> findByProfileIdOrderByCreatedAtAscIdAsc(String profileId);

    List<Memory> findByProfileIdAndEnabledTrueOrderByCreatedAtAscIdAsc(String profileId);

    Optional<Memory> findByIdAndProfileId(String id, String profileId);

    Optional<Memory> findByIdAndProfileIdAndEnabledTrue(String id, String profileId);
}
