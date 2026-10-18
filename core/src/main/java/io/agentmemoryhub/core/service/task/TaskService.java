package io.agentmemoryhub.core.service.task;

import io.agentmemoryhub.core.model.record.TaskAnalysisRecord;
import io.agentmemoryhub.core.model.record.TaskContinuityRecord;
import io.agentmemoryhub.core.model.record.TaskContextRecord;
import io.agentmemoryhub.core.model.record.TaskInputRecord;
import io.agentmemoryhub.core.model.record.TaskPageRecord;
import io.agentmemoryhub.core.model.record.TaskRecord;
import io.agentmemoryhub.core.model.record.WorkspaceReferenceRecord;

public interface TaskService {
    TaskPageRecord listTasksByProfileId(String profileId, int page, int pageSize);

    TaskRecord getTaskById(String taskId);

    TaskRecord createTaskByProfileIdAndInput(String profileId, TaskInputRecord taskInputRecord, String requestId);

    TaskRecord updateTaskById(String taskId, TaskInputRecord taskInputRecord, Long expectedVersion);

    void deleteTaskById(String taskId, boolean confirm);

    TaskContextRecord getTaskContextByProjectIdAndTaskId(String projectId, String taskId, String prompt);

    WorkspaceReferenceRecord getWorkspaceReferenceGuidanceByProjectIdAndTaskId(String projectId, String taskId);

    TaskAnalysisRecord analyzeTaskPromptByProjectIdAndTaskId(String projectId, String prompt, String taskId);

    TaskContinuityRecord inspectTaskContinuityByProjectIdAndTaskId(String projectId, String taskId);
}
