package io.agentmemoryhub.webapp.controller.profile;

import io.agentmemoryhub.core.model.record.ProfileInputRecord;
import io.agentmemoryhub.core.model.record.ProfilePageRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.service.profile.ProfileService;
import io.agentmemoryhub.core.service.project.ProjectService;
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
@RequestMapping("/projects/{projectId}")
public class ProfileController {
    private final ProfileService profileService;
    private final ProjectService projectService;

    @GetMapping("/profiles")
    public ResponseEntity<ProfilePageRecord> listProfiles(@PathVariable String projectId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int pageSize) {
        return ResponseEntity.ok(profileService.listProfilesByProjectId(projectId, page, pageSize));
    }

    @PostMapping("/profiles")
    public ResponseEntity<ProfileRecord> createProfile(@PathVariable String projectId, @RequestBody ProfileInputRecord profileInputRecord) {
        return ResponseEntity.ok(profileService.createProfileByProjectIdAndInput(projectId, profileInputRecord));
    }

    @GetMapping("/profiles/{profileId}")
    public ResponseEntity<ProfileRecord> getProfile(@PathVariable String projectId, @PathVariable String profileId) {
        return ResponseEntity.ok(profileService.getProfileByIdAndProjectId(profileId, projectId));
    }

    @PutMapping("/profiles/{profileId}")
    public ResponseEntity<ProfileRecord> updateProfile(@PathVariable String projectId, @PathVariable String profileId, @RequestBody ProfileInputRecord profileInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        return ResponseEntity.ok(profileService.updateProfileByIdAndProjectId(profileId, projectId, profileInputRecord, expectedVersion));
    }

    @DeleteMapping("/profiles/{profileId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String projectId, @PathVariable String profileId) {
        profileService.deleteProfileByIdAndProjectId(profileId, projectId, true);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/active-profile/{profileId}")
    public ResponseEntity<ProfileRecord> selectActiveProfile(@PathVariable String projectId, @PathVariable String profileId) {
        return ResponseEntity.ok(projectService.selectActiveProfileByProjectIdAndProfileId(projectId, profileId));
    }
}
