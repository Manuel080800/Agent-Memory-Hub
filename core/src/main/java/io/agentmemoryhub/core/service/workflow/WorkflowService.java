package io.agentmemoryhub.core.service.workflow;

import io.agentmemoryhub.core.model.record.WorkflowInputRecord;
import io.agentmemoryhub.core.model.record.WorkflowPageRecord;
import io.agentmemoryhub.core.model.record.WorkflowRecord;

public interface WorkflowService {
    WorkflowPageRecord listWorkflowsByProfileId(String profileId, int page, int pageSize);

    WorkflowRecord getWorkflowByIdAndProfileId(String workflowId, String profileId);

    WorkflowRecord createWorkflowByProfileIdAndInput(String profileId, WorkflowInputRecord workflowInputRecord, String requestId);

    WorkflowRecord updateWorkflowByIdAndProfileId(String workflowId, String profileId, WorkflowInputRecord workflowInputRecord, Long expectedVersion);

    void deleteWorkflowByIdAndProfileId(String workflowId, String profileId, boolean confirm);
}
