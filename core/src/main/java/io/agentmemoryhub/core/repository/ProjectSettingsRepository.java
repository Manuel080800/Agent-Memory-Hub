package io.agentmemoryhub.core.repository;

import io.agentmemoryhub.core.model.domain.ProjectSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSettingsRepository extends JpaRepository<ProjectSettings, String> {
}
