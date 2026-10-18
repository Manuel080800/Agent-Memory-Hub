package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.core.model.record.ContextResourceRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.ProjectContextRecord;
import io.agentmemoryhub.core.service.context.ContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}")
public class ContextController {
    private final ContextService contextService;

    @GetMapping("/active-profile")
    public ProfileRecord getActiveProfile(@PathVariable String projectId) {
        return contextService.getActiveProfileByProjectId(projectId);
    }

    @GetMapping("/context")
    public ProjectContextRecord getProjectContext(@PathVariable String projectId, @RequestParam(required = false) String prompt) {
        return contextService.getProjectContextByProjectIdAndPrompt(projectId, prompt);
    }

    @GetMapping("/context/{resourceType}/{resourceId}")
    public ContextResourceRecord getContextResource(@PathVariable String projectId, @PathVariable String resourceType, @PathVariable String resourceId) {
        return contextService.getContextResourceByProjectIdAndTypeAndResourceId(projectId, resourceType, resourceId);
    }
}
