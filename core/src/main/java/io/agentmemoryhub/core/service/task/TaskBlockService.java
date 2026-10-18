package io.agentmemoryhub.core.service.task;

import io.agentmemoryhub.core.model.record.TaskBlockInputRecord;
import io.agentmemoryhub.core.model.record.TaskBlockPageRecord;
import io.agentmemoryhub.core.model.record.TaskBlockRecord;

public interface TaskBlockService {
    TaskBlockPageRecord listTaskBlocksByTaskId(String taskId, int page, int pageSize);

    TaskBlockRecord getTaskBlockById(String blockId);

    TaskBlockRecord createTaskBlockByTaskIdAndInput(String taskId, TaskBlockInputRecord taskBlockInputRecord, String requestId);

    TaskBlockRecord updateTaskBlockById(String blockId, TaskBlockInputRecord taskBlockInputRecord, Long expectedVersion);

    void deleteTaskBlockById(String blockId, boolean confirm);
}
