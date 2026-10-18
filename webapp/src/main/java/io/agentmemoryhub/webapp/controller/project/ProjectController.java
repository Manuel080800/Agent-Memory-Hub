package io.agentmemoryhub.webapp.controller.project;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.ProjectInputRecord;
import io.agentmemoryhub.core.model.record.ProjectPageRecord;
import io.agentmemoryhub.core.model.record.ProjectRecord;
import io.agentmemoryhub.core.service.project.ProjectService;
import io.agentmemoryhub.webapp.handler.session.WebAppSessionHandler;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final WebAppSessionHandler webAppSessionHandler;

    @GetMapping
    public ResponseEntity<ProjectPageRecord> listProjects(HttpSession httpSession, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int pageSize) {
        var userRecord = webAppSessionHandler.getUser(httpSession);
        return ResponseEntity.ok(projectService.listProjectsByUserId(userRecord.id(), page, pageSize));
    }

    @PostMapping
    public ResponseEntity<ProjectRecord> createProject(HttpSession httpSession, @RequestBody ProjectInputRecord projectInputRecord) {
        var userRecord = webAppSessionHandler.getUser(httpSession);

        if (Objects.isNull(projectInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "project");
        }

        var ownedProjectInputRecord = new ProjectInputRecord(projectInputRecord.name(), projectInputRecord.description(), projectInputRecord.enabled(), userRecord.id());
        return ResponseEntity.ok(projectService.createProjectByInput(ownedProjectInputRecord));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectRecord> getProject(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectRecord> updateProject(@PathVariable String projectId, @RequestBody ProjectInputRecord projectInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        return ResponseEntity.ok(projectService.updateProjectById(projectId, projectInputRecord, expectedVersion));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable String projectId, @RequestParam(defaultValue = "false") boolean confirm) {
        projectService.deleteProjectById(projectId, confirm);
        return ResponseEntity.noContent().build();
    }
}
