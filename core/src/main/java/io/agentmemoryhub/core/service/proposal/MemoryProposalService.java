package io.agentmemoryhub.core.service.proposal;

import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalPageRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalRecord;
import io.agentmemoryhub.core.model.record.MemoryRecord;

public interface MemoryProposalService {
    MemoryProposalPageRecord listMemoryProposalsByTaskId(String taskId, int page, int pageSize);

    MemoryProposalRecord getMemoryProposalById(String proposalId);

    MemoryProposalRecord createMemoryProposalByTaskIdAndInput(String taskId, MemoryProposalInputRecord memoryProposalInputRecord, String requestId);

    MemoryProposalRecord updateMemoryProposalById(String proposalId, MemoryProposalInputRecord memoryProposalInputRecord, Long expectedVersion);

    void deleteMemoryProposalById(String proposalId, boolean confirm);

    MemoryRecord approveMemoryProposalById(String proposalId, MemoryInputRecord memoryInputRecord);
}
