package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ApplicationMode;
import java.util.List;

public record MemoryInputRecord(
        String title,
        String content,
        List<String> tags,
        ApplicationMode applicationMode,
        Boolean enabled
) {}
