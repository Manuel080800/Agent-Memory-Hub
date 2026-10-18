package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.model.domain.Skill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, String> {
    List<Skill> findByProfileIdOrderByCreatedAtAscIdAsc(String profileId);

    List<Skill> findByProfileIdAndEnabledTrueOrderByCreatedAtAscIdAsc(String profileId);

    Optional<Skill> findByIdAndProfileId(String id, String profileId);

    Optional<Skill> findByIdAndProfileIdAndEnabledTrue(String id, String profileId);
}
