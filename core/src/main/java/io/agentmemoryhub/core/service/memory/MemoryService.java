package io.agentmemoryhub.core.service.memory;

import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.MemoryPageRecord;
import io.agentmemoryhub.core.model.record.MemoryRecord;

public interface MemoryService {
    MemoryPageRecord listMemoriesByProfileId(String profileId, int page, int pageSize);

    MemoryRecord getMemoryByIdAndProfileId(String memoryId, String profileId);

    MemoryRecord createMemoryByProfileIdAndInput(String profileId, MemoryInputRecord memoryInputRecord, String requestId);

    MemoryRecord updateMemoryByIdAndProfileId(String memoryId, String profileId, MemoryInputRecord memoryInputRecord, Long expectedVersion);

    void deleteMemoryByIdAndProfileId(String memoryId, String profileId, boolean confirm);
}
