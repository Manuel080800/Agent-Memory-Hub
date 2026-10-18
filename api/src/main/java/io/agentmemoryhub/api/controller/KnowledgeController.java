package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.MemoryPageRecord;
import io.agentmemoryhub.core.model.record.MemoryRecord;
import io.agentmemoryhub.core.model.record.SkillInputRecord;
import io.agentmemoryhub.core.model.record.SkillPageRecord;
import io.agentmemoryhub.core.model.record.SkillRecord;
import io.agentmemoryhub.core.model.record.WorkflowInputRecord;
import io.agentmemoryhub.core.model.record.WorkflowPageRecord;
import io.agentmemoryhub.core.model.record.WorkflowRecord;
import io.agentmemoryhub.core.service.memory.MemoryService;
import io.agentmemoryhub.core.service.skill.SkillService;
import io.agentmemoryhub.core.service.workflow.WorkflowService;
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
@RequestMapping("/api/v1/projects/{projectId}/profiles/{profileId}")
public class KnowledgeController {
    private final SkillService skillService;
    private final WorkflowService workflowService;
    private final MemoryService memoryService;

    @GetMapping("/skills")
    public SkillPageRecord listSkills(@PathVariable String profileId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return skillService.listSkillsByProfileId(profileId, page, pageSize);
    }

    @PostMapping("/skills")
    public SkillRecord createSkill(@PathVariable String profileId, @RequestHeader(name = "Idempotency-Key", required = false) String requestId, @RequestBody SkillInputRecord skillInputRecord) {
        return skillService.createSkillByProfileIdAndInput(profileId, skillInputRecord, requestId);
    }

    @GetMapping("/skills/{skillId}")
    public SkillRecord getSkill(@PathVariable String profileId, @PathVariable String skillId) {
        return skillService.getSkillByIdAndProfileId(skillId, profileId);
    }

    @PutMapping("/skills/{skillId}")
    public SkillRecord updateSkill(@PathVariable String profileId, @PathVariable String skillId, @RequestHeader(name = "If-Match", required = false) Long expectedVersion, @RequestBody SkillInputRecord skillInputRecord) {
        return skillService.updateSkillByIdAndProfileId(skillId, profileId, skillInputRecord, expectedVersion);
    }

    @DeleteMapping("/skills/{skillId}")
    public void deleteSkill(@PathVariable String profileId, @PathVariable String skillId, @RequestParam(defaultValue = "false") boolean confirm) {
        skillService.deleteSkillByIdAndProfileId(skillId, profileId, confirm);
    }

    @GetMapping("/workflows")
    public WorkflowPageRecord listWorkflows(@PathVariable String profileId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return workflowService.listWorkflowsByProfileId(profileId, page, pageSize);
    }

    @PostMapping("/workflows")
    public WorkflowRecord createWorkflow(@PathVariable String profileId, @RequestHeader(name = "Idempotency-Key", required = false) String requestId, @RequestBody WorkflowInputRecord workflowInputRecord) {
        return workflowService.createWorkflowByProfileIdAndInput(profileId, workflowInputRecord, requestId);
    }

    @GetMapping("/workflows/{workflowId}")
    public WorkflowRecord getWorkflow(@PathVariable String profileId, @PathVariable String workflowId) {
        return workflowService.getWorkflowByIdAndProfileId(workflowId, profileId);
    }

    @PutMapping("/workflows/{workflowId}")
    public WorkflowRecord updateWorkflow(@PathVariable String profileId, @PathVariable String workflowId, @RequestHeader(name = "If-Match", required = false) Long expectedVersion, @RequestBody WorkflowInputRecord workflowInputRecord) {
        return workflowService.updateWorkflowByIdAndProfileId(workflowId, profileId, workflowInputRecord, expectedVersion);
    }

    @DeleteMapping("/workflows/{workflowId}")
    public void deleteWorkflow(@PathVariable String profileId, @PathVariable String workflowId, @RequestParam(defaultValue = "false") boolean confirm) {
        workflowService.deleteWorkflowByIdAndProfileId(workflowId, profileId, confirm);
    }

    @GetMapping("/memories")
    public MemoryPageRecord listMemories(@PathVariable String profileId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return memoryService.listMemoriesByProfileId(profileId, page, pageSize);
    }

    @PostMapping("/memories")
    public MemoryRecord createMemory(@PathVariable String profileId, @RequestHeader(name = "Idempotency-Key", required = false) String requestId, @RequestBody MemoryInputRecord memoryInputRecord) {
        return memoryService.createMemoryByProfileIdAndInput(profileId, memoryInputRecord, requestId);
    }

    @GetMapping("/memories/{memoryId}")
    public MemoryRecord getMemory(@PathVariable String profileId, @PathVariable String memoryId) {
        return memoryService.getMemoryByIdAndProfileId(memoryId, profileId);
    }

    @PutMapping("/memories/{memoryId}")
    public MemoryRecord updateMemory(@PathVariable String profileId, @PathVariable String memoryId, @RequestHeader(name = "If-Match", required = false) Long expectedVersion, @RequestBody MemoryInputRecord memoryInputRecord) {
        return memoryService.updateMemoryByIdAndProfileId(memoryId, profileId, memoryInputRecord, expectedVersion);
    }

    @DeleteMapping("/memories/{memoryId}")
    public void deleteMemory(@PathVariable String profileId, @PathVariable String memoryId, @RequestParam(defaultValue = "false") boolean confirm) {
        memoryService.deleteMemoryByIdAndProfileId(memoryId, profileId, confirm);
    }
}
