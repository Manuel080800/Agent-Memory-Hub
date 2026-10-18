package io.agentmemoryhub.webapp.controller.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.SkillInputRecord;
import io.agentmemoryhub.core.model.record.TaskInputRecord;
import io.agentmemoryhub.core.model.record.TaskRecord;
import io.agentmemoryhub.core.model.record.WorkflowInputRecord;
import io.agentmemoryhub.core.service.memory.MemoryService;
import io.agentmemoryhub.core.service.profile.ProfileService;
import io.agentmemoryhub.core.service.skill.SkillService;
import io.agentmemoryhub.core.service.task.TaskService;
import io.agentmemoryhub.core.service.workflow.WorkflowService;
import java.util.Locale;
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
@RequestMapping("/projects/{projectId}/profiles/{profileId}")
public class ProfileResourceController {
    private final ProfileService profileService;
    private final SkillService skillService;
    private final WorkflowService workflowService;
    private final MemoryService memoryService;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{resourceType}")
    public ResponseEntity<?> listResources(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String resourceType, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int pageSize) {
        validateProfileBelongsToProject(projectId, profileId);
        var normalizedResourceType = resourceType.toUpperCase(Locale.ROOT);
        var resourcePage = switch (normalizedResourceType) {
            case "SKILLS" -> skillService.listSkillsByProfileId(profileId, page, pageSize);
            case "WORKFLOWS" -> workflowService.listWorkflowsByProfileId(profileId, page, pageSize);
            case "MEMORIES" -> memoryService.listMemoriesByProfileId(profileId, page, pageSize);
            case "TASKS" -> taskService.listTasksByProfileId(profileId, page, pageSize);
            default -> throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "resourceType");
        };
        return ResponseEntity.ok(resourcePage);
    }

    @PostMapping("/resources/{resourceType}")
    public ResponseEntity<?> createResource(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String resourceType, @RequestBody JsonNode requestNode, @RequestHeader(name = "Idempotency-Key", required = false) String requestId) {
        validateProfileBelongsToProject(projectId, profileId);
        var normalizedResourceType = resourceType.toUpperCase(Locale.ROOT);
        var resourceRecord = switch (normalizedResourceType) {
            case "SKILL" -> skillService.createSkillByProfileIdAndInput(profileId, objectMapper.convertValue(requestNode, SkillInputRecord.class), requestId);
            case "WORKFLOW" -> workflowService.createWorkflowByProfileIdAndInput(profileId, objectMapper.convertValue(requestNode, WorkflowInputRecord.class), requestId);
            case "MEMORY" -> memoryService.createMemoryByProfileIdAndInput(profileId, objectMapper.convertValue(requestNode, MemoryInputRecord.class), requestId);
            case "TASK" -> taskService.createTaskByProfileIdAndInput(profileId, objectMapper.convertValue(requestNode, TaskInputRecord.class), requestId);
            default -> throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "resourceType");
        };
        return ResponseEntity.ok(resourceRecord);
    }

    @PutMapping("/{resourceType}/{resourceId}")
    public ResponseEntity<?> updateResource(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String resourceType, @PathVariable String resourceId, @RequestBody JsonNode requestNode, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        validateProfileBelongsToProject(projectId, profileId);
        var normalizedResourceType = resourceType.toUpperCase(Locale.ROOT);
        var resourceRecord = switch (normalizedResourceType) {
            case "SKILLS" -> skillService.updateSkillByIdAndProfileId(resourceId, profileId, objectMapper.convertValue(requestNode, SkillInputRecord.class), expectedVersion);
            case "WORKFLOWS" -> workflowService.updateWorkflowByIdAndProfileId(resourceId, profileId, objectMapper.convertValue(requestNode, WorkflowInputRecord.class), expectedVersion);
            case "MEMORIES" -> memoryService.updateMemoryByIdAndProfileId(resourceId, profileId, objectMapper.convertValue(requestNode, MemoryInputRecord.class), expectedVersion);
            case "TASKS" -> updateTaskByProfileIdAndId(profileId, resourceId, objectMapper.convertValue(requestNode, TaskInputRecord.class), expectedVersion);
            default -> throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "resourceType");
        };
        return ResponseEntity.ok(resourceRecord);
    }

    @DeleteMapping("/{resourceType}/{resourceId}")
    public ResponseEntity<Void> deleteResource(@PathVariable String projectId, @PathVariable String profileId, @PathVariable String resourceType, @PathVariable String resourceId, @RequestParam(defaultValue = "false") boolean confirm) {
        validateProfileBelongsToProject(projectId, profileId);
        var normalizedResourceType = resourceType.toUpperCase(Locale.ROOT);

        switch (normalizedResourceType) {
            case "SKILLS" -> skillService.deleteSkillByIdAndProfileId(resourceId, profileId, confirm);
            case "WORKFLOWS" -> workflowService.deleteWorkflowByIdAndProfileId(resourceId, profileId, confirm);
            case "MEMORIES" -> memoryService.deleteMemoryByIdAndProfileId(resourceId, profileId, confirm);
            case "TASKS" -> deleteTaskByProfileIdAndId(profileId, resourceId, confirm);
            default -> throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "resourceType");
        }

        return ResponseEntity.noContent().build();
    }

    private ProfileRecord validateProfileBelongsToProject(String projectId, String profileId) {
        var profileRecord = profileService.getProfileByIdAndProjectId(profileId, projectId);

        if (!projectId.equals(profileRecord.projectId())) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, profileId);
        }

        return profileRecord;
    }

    private TaskRecord findTaskByProfileIdAndId(String profileId, String taskId) {
        var taskRecord = taskService.getTaskById(taskId);

        if (!profileId.equals(taskRecord.profileId())) {
            throw new HubException("INVALID_RELATION", "error.invalidRelation", 422, taskId);
        }

        return taskRecord;
    }

    private TaskRecord updateTaskByProfileIdAndId(String profileId, String taskId, TaskInputRecord taskInputRecord, Long expectedVersion) {
        findTaskByProfileIdAndId(profileId, taskId);
        return taskService.updateTaskById(taskId, taskInputRecord, expectedVersion);
    }

    private void deleteTaskByProfileIdAndId(String profileId, String taskId, boolean confirm) {
        findTaskByProfileIdAndId(profileId, taskId);
        taskService.deleteTaskById(taskId, confirm);
    }

}
