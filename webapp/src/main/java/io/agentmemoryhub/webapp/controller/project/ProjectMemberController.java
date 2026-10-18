package io.agentmemoryhub.webapp.controller.project;

import io.agentmemoryhub.core.model.record.ProjectMemberInputRecord;
import io.agentmemoryhub.core.model.record.ProjectMemberPageRecord;
import io.agentmemoryhub.core.model.record.ProjectMemberRecord;
import io.agentmemoryhub.core.service.member.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;

    @GetMapping
    public ResponseEntity<ProjectMemberPageRecord> listMembers(@PathVariable String projectId) {
        return ResponseEntity.ok(projectMemberService.listProjectMembersByProjectId(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectMemberRecord> addMember(@PathVariable String projectId, @RequestBody ProjectMemberInputRecord projectMemberInputRecord) {
        return ResponseEntity.ok(projectMemberService.addProjectMemberByProjectIdAndInput(projectId, projectMemberInputRecord));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ProjectMemberRecord> updateMember(@PathVariable String projectId, @PathVariable String userId, @RequestBody ProjectMemberInputRecord projectMemberInputRecord) {
        return ResponseEntity.ok(projectMemberService.updateProjectMemberByProjectIdAndUserId(projectId, userId, projectMemberInputRecord));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable String projectId, @PathVariable String userId, @RequestParam(defaultValue = "false") boolean confirm) {
        projectMemberService.removeProjectMemberByProjectIdAndUserId(projectId, userId, confirm);
        return ResponseEntity.noContent().build();
    }
}
