package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.ProfileInputRecord;
import io.agentmemoryhub.core.model.record.ProfilePageRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.ProjectInputRecord;
import io.agentmemoryhub.core.model.record.ProjectPageRecord;
import io.agentmemoryhub.core.model.record.ProjectRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.profile.ProfileService;
import io.agentmemoryhub.core.service.project.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProjectController {
    private final ProjectService projectService;
    private final ProfileService profileService;

    @GetMapping("/projects")
    public ProjectPageRecord listProjects(HttpServletRequest httpServletRequest, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        var userRecord = (UserRecord) httpServletRequest.getAttribute("authenticatedUser");
        return projectService.listProjectsByUserId(userRecord.id(), page, pageSize);
    }

    @PostMapping("/projects")
    public ProjectRecord createProject(HttpServletRequest httpServletRequest, @RequestBody ProjectInputRecord projectInputRecord) {
        var userRecord = (UserRecord) httpServletRequest.getAttribute("authenticatedUser");

        if (Objects.isNull(projectInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "project");
        }

        var ownedProjectInputRecord = new ProjectInputRecord(projectInputRecord.name(), projectInputRecord.description(), projectInputRecord.enabled(), userRecord.id());
        return projectService.createProjectByInput(ownedProjectInputRecord);
    }

    @GetMapping("/projects/{projectId}")
    public ProjectRecord getProject(@PathVariable String projectId) {
        return projectService.getProjectById(projectId);
    }

    @PutMapping("/projects/{projectId}")
    public ProjectRecord updateProject(@PathVariable String projectId, @RequestBody ProjectInputRecord projectInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        return projectService.updateProjectById(projectId, projectInputRecord, expectedVersion);
    }

    @DeleteMapping("/projects/{projectId}")
    public void deleteProject(@PathVariable String projectId, @RequestParam(defaultValue = "false") boolean confirm) {
        projectService.deleteProjectById(projectId, confirm);
    }

    @GetMapping("/projects/{projectId}/profiles")
    public ProfilePageRecord listProfiles(@PathVariable String projectId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return profileService.listProfilesByProjectId(projectId, page, pageSize);
    }

    @PostMapping("/projects/{projectId}/profiles")
    public ProfileRecord createProfile(@PathVariable String projectId, @RequestBody ProfileInputRecord profileInputRecord) {
        return profileService.createProfileByProjectIdAndInput(projectId, profileInputRecord);
    }

    @GetMapping("/projects/{projectId}/profiles/{profileId}")
    public ProfileRecord getProfile(@PathVariable String projectId, @PathVariable String profileId) {
        return profileService.getProfileByIdAndProjectId(profileId, projectId);
    }

    @PutMapping("/projects/{projectId}/profiles/{profileId}")
    public ProfileRecord updateProfile(@PathVariable String projectId, @PathVariable String profileId, @RequestBody ProfileInputRecord profileInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        return profileService.updateProfileByIdAndProjectId(profileId, projectId, profileInputRecord, expectedVersion);
    }

    @DeleteMapping("/projects/{projectId}/profiles/{profileId}")
    public void deleteProfile(@PathVariable String projectId, @PathVariable String profileId, @RequestParam(defaultValue = "false") boolean confirm) {
        profileService.deleteProfileByIdAndProjectId(profileId, projectId, confirm);
    }

    @PutMapping("/projects/{projectId}/active-profile/{profileId}")
    public ProfileRecord selectActiveProfile(@PathVariable String projectId, @PathVariable String profileId) {
        return projectService.selectActiveProfileByProjectIdAndProfileId(projectId, profileId);
    }
}
