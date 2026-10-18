package io.agentmemoryhub.core.model.record;

import java.util.List;

public record ErrorResponseRecord(
        String code,
        String message,
        List<ErrorDetailRecord> details
) {}
