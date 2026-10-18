package io.agentmemoryhub.core.service.context;

import io.agentmemoryhub.core.model.record.ContextResourceRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.ProjectContextRecord;

public interface ContextService {
    ProfileRecord getActiveProfileByProjectId(String projectId);

    ProjectContextRecord getProjectContextByProjectIdAndPrompt(String projectId, String prompt);

    ContextResourceRecord getContextResourceByProjectIdAndTypeAndResourceId(String projectId, String resourceType, String resourceId);
}
