package io.agentmemoryhub.core.model.record;

import io.agentmemoryhub.core.common.ApplicationMode;
import java.util.List;

public record ContextResourceRecord(
        String resourceType,
        String id,
        String name,
        String title,
        String description,
        String content,
        String instructions,
        List<String> tags,
        ApplicationMode applicationMode,
        boolean truncated,
        int estimatedTokens,
        String fullReadTool
) {}
