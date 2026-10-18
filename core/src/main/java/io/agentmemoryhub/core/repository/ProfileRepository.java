package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.model.domain.Profile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, String> {
    List<Profile> findByProjectIdOrderByCreatedAtAscIdAsc(String projectId);

    List<Profile> findByProjectIdAndEnabledTrueOrderByCreatedAtAscIdAsc(String projectId);

    Optional<Profile> findByIdAndProjectId(String id, String projectId);

    Optional<Profile> findByIdAndProjectIdAndEnabledTrue(String id, String projectId);

    Optional<Profile> findByIdAndEnabledTrue(String id);
}
