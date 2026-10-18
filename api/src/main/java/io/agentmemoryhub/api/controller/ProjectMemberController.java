package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.core.model.record.ProjectMemberInputRecord;
import io.agentmemoryhub.core.model.record.ProjectMemberPageRecord;
import io.agentmemoryhub.core.model.record.ProjectMemberRecord;
import io.agentmemoryhub.core.service.member.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;

    @GetMapping
    public ProjectMemberPageRecord listMembers(@PathVariable String projectId) {
        return projectMemberService.listProjectMembersByProjectId(projectId);
    }

    @PostMapping
    public ProjectMemberRecord addMember(@PathVariable String projectId, @RequestBody ProjectMemberInputRecord inputRecord) {
        return projectMemberService.addProjectMemberByProjectIdAndInput(projectId, inputRecord);
    }

    @PutMapping("/{userId}")
    public ProjectMemberRecord updateMember(@PathVariable String projectId, @PathVariable String userId, @RequestBody ProjectMemberInputRecord inputRecord) {
        return projectMemberService.updateProjectMemberByProjectIdAndUserId(projectId, userId, inputRecord);
    }

    @DeleteMapping("/{userId}")
    public void removeMember(@PathVariable String projectId, @PathVariable String userId, @RequestParam(defaultValue = "false") boolean confirm) {
        projectMemberService.removeProjectMemberByProjectIdAndUserId(projectId, userId, confirm);
    }
}
