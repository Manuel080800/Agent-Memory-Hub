package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.core.model.record.ProjectSettingsInputRecord;
import io.agentmemoryhub.core.model.record.ProjectSettingsRecord;
import io.agentmemoryhub.core.service.settings.ProjectSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/settings")
public class ProjectSettingsController {
    private final ProjectSettingsService projectSettingsService;

    @GetMapping
    public ProjectSettingsRecord getProjectSettings(@PathVariable String projectId) {
        return projectSettingsService.getProjectSettingsByProjectId(projectId);
    }

    @PutMapping
    public ProjectSettingsRecord updateProjectSettings(@PathVariable String projectId, @RequestBody ProjectSettingsInputRecord projectSettingsInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        return projectSettingsService.updateProjectSettingsByProjectId(projectId, projectSettingsInputRecord, expectedVersion);
    }

    @PostMapping("/reset")
    public ProjectSettingsRecord resetProjectSettings(@PathVariable String projectId) {
        return projectSettingsService.resetProjectSettingsByProjectId(projectId);
    }
}
