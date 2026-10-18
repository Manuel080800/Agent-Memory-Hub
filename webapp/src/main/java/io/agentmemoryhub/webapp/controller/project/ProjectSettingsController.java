package io.agentmemoryhub.webapp.controller.project;

import io.agentmemoryhub.core.model.record.ProjectSettingsInputRecord;
import io.agentmemoryhub.core.model.record.ProjectSettingsRecord;
import io.agentmemoryhub.core.service.settings.ProjectSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/settings")
public class ProjectSettingsController {
    private final ProjectSettingsService projectSettingsService;

    @GetMapping
    public ResponseEntity<ProjectSettingsRecord> getProjectSettings(@PathVariable String projectId) {
        return ResponseEntity.ok(projectSettingsService.getProjectSettingsByProjectId(projectId));
    }

    @PutMapping
    public ResponseEntity<ProjectSettingsRecord> updateProjectSettings(@PathVariable String projectId, @RequestBody ProjectSettingsInputRecord projectSettingsInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        return ResponseEntity.ok(projectSettingsService.updateProjectSettingsByProjectId(projectId, projectSettingsInputRecord, expectedVersion));
    }

    @PostMapping("/reset")
    public ResponseEntity<ProjectSettingsRecord> resetProjectSettings(@PathVariable String projectId) {
        return ResponseEntity.ok(projectSettingsService.resetProjectSettingsByProjectId(projectId));
    }
}
