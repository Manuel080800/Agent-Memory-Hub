package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ApplicationMode;
import java.util.List;

public record SkillInputRecord(
        String name,
        String description,
        String instructions,
        List<String> tags,
        ApplicationMode applicationMode,
        Boolean enabled
) {}
