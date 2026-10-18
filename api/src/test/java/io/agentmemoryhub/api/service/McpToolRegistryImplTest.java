package io.agentmemoryhub.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.api.common.McpEndpointType;
import io.agentmemoryhub.api.model.McpToolDefinitionRecord;
import io.agentmemoryhub.core.common.TaskCreationMode;
import io.agentmemoryhub.core.model.record.ProjectSettingsRecord;
import io.agentmemoryhub.core.service.context.ContextService;
import io.agentmemoryhub.core.service.memory.MemoryService;
import io.agentmemoryhub.core.service.profile.ProfileService;
import io.agentmemoryhub.core.service.project.ProjectService;
import io.agentmemoryhub.core.service.proposal.MemoryProposalService;
import io.agentmemoryhub.core.service.settings.ProjectSettingsService;
import io.agentmemoryhub.core.service.skill.SkillService;
import io.agentmemoryhub.core.service.task.TaskBlockService;
import io.agentmemoryhub.core.service.task.TaskService;
import io.agentmemoryhub.core.service.workflow.WorkflowService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class McpToolRegistryImplTest {

    private ContextService contextService;
    private ProjectService projectService;
    private ProjectSettingsService projectSettingsService;
    private ProfileService profileService;
    private SkillService skillService;
    private WorkflowService workflowService;
    private MemoryService memoryService;
    private MemoryProposalService memoryProposalService;
    private TaskService taskService;
    private TaskBlockService taskBlockService;
    private McpToolRegistryImpl mcpToolRegistry;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        projectService = mock(ProjectService.class);
        projectSettingsService = mock(ProjectSettingsService.class);
        profileService = mock(ProfileService.class);
        skillService = mock(SkillService.class);
        workflowService = mock(WorkflowService.class);
        memoryService = mock(MemoryService.class);
        memoryProposalService = mock(MemoryProposalService.class);
        taskService = mock(TaskService.class);
        taskBlockService = mock(TaskBlockService.class);
        mcpToolRegistry = new McpToolRegistryImpl(contextService, projectService, projectSettingsService, profileService, skillService, workflowService, memoryService, memoryProposalService, taskService, taskBlockService, new ObjectMapper(), new StaticMessageSource());
    }

    @Test
    void publishesSeparateContextAndTaskToolCatalogs() {
        // Verifies that both MCP endpoints publish the complete, independent tool catalogs.
        var contextToolList = mcpToolRegistry.listToolsByEndpoint(McpEndpointType.CONTEXT, Locale.ENGLISH);
        var taskToolList = mcpToolRegistry.listToolsByEndpoint(McpEndpointType.TASKS, Locale.ENGLISH);

        assertEquals(35, contextToolList.size());
        assertEquals(15, taskToolList.size());
        assertTrue(contextToolList.stream().anyMatch(tool -> "get_project_settings".equals(tool.name())));
        assertTrue(taskToolList.stream().anyMatch(tool -> "get_project_settings".equals(tool.name())));
        assertTrue(contextToolList.stream().anyMatch(tool -> "propose_memory".equals(tool.name())));
        assertFalse(contextToolList.stream().anyMatch(tool -> "create_task".equals(tool.name())));
        assertTrue(taskToolList.stream().anyMatch(tool -> "create_task".equals(tool.name())));
        assertFalse(taskToolList.stream().anyMatch(tool -> "approve_memory_proposal".equals(tool.name())));
    }

    @Test
    void publishesSpecificOutputSchemasAndStrictInputSchemas() {
        // Verifies each tool publishes a typed data schema and rejects extra input properties.
        var allToolList = new java.util.ArrayList<McpToolDefinitionRecord>();
        allToolList.addAll(mcpToolRegistry.listToolsByEndpoint(McpEndpointType.CONTEXT, Locale.ENGLISH));
        allToolList.addAll(mcpToolRegistry.listToolsByEndpoint(McpEndpointType.TASKS, Locale.ENGLISH));

        for (var toolDefinitionRecord : allToolList) {
            var inputSchema = toolDefinitionRecord.inputSchema();
            var outputSchema = toolDefinitionRecord.outputSchema();
            assertFalse(Boolean.TRUE.equals(inputSchema.get("additionalProperties")));
            assertTrue(outputSchema.containsKey("oneOf"));
        }

        var createTaskTool = findToolByName(allToolList, "create_task");
        var createTaskSuccessSchema = successSchema(createTaskTool);
        var createTaskDataSchema = objectValue(objectValue(createTaskSuccessSchema.get("properties")).get("data"));
        var createTaskDataProperties = objectValue(createTaskDataSchema.get("properties"));
        assertTrue(createTaskDataProperties.containsKey("taskId"));
        assertTrue(createTaskDataProperties.containsKey("nextSteps"));

        var projectPageTool = findToolByName(allToolList, "list_projects");
        var projectPageDataSchema = objectValue(objectValue(successSchema(projectPageTool).get("properties")).get("data"));
        assertTrue(objectValue(projectPageDataSchema.get("properties")).containsKey("items"));
    }

    @Test
    void readsSharedProjectSettingsFromBothIndependentMcpServers() {
        // Verifies both MCP servers expose the same project settings through the shared Core service.
        var projectSettingsRecord = new ProjectSettingsRecord("project-1", true, true, true, TaskCreationMode.MANUAL, false, true, 6000, 70, false, null, 0L);
        var arguments = new ObjectMapper().createObjectNode().put("projectId", "project-1");
        when(projectSettingsService.getProjectSettingsByProjectId("project-1")).thenReturn(projectSettingsRecord);

        var contextResult = mcpToolRegistry.callToolByEndpointAndNameAndArguments(McpEndpointType.CONTEXT, "get_project_settings", arguments, Locale.ENGLISH);
        var taskResult = mcpToolRegistry.callToolByEndpointAndNameAndArguments(McpEndpointType.TASKS, "get_project_settings", arguments, Locale.ENGLISH);
        var contextContent = objectValue(contextResult.structuredContent());
        var taskContent = objectValue(taskResult.structuredContent());

        assertFalse(contextResult.isError());
        assertFalse(taskResult.isError());
        assertEquals(projectSettingsRecord, contextContent.get("data"));
        assertEquals(projectSettingsRecord, taskContent.get("data"));
        verify(projectSettingsService, times(2)).getProjectSettingsByProjectId("project-1");
    }

    @Test
    void rejectsUnknownFieldsDuplicateTagsAndEmptyPartialUpdates() {
        // Verifies schema constraints reject malformed writes before calling business services.
        var unknownFieldArguments = new ObjectMapper().createObjectNode().put("profileId", "profile-1").put("name", "Skill").put("instructions", "Use this skill").put("unexpected", "value");
        var duplicateTagsArguments = new ObjectMapper().createObjectNode().put("profileId", "profile-1").put("name", "Skill").put("instructions", "Use this skill").set("tags", new ObjectMapper().createArrayNode().add("java").add("java"));
        var emptyChangesArguments = new ObjectMapper().createObjectNode().put("taskId", "task-1").set("changes", new ObjectMapper().createObjectNode());

        assertTrue(mcpToolRegistry.callToolByEndpointAndNameAndArguments(McpEndpointType.CONTEXT, "create_skill", unknownFieldArguments, Locale.ENGLISH).isError());
        assertTrue(mcpToolRegistry.callToolByEndpointAndNameAndArguments(McpEndpointType.CONTEXT, "create_skill", duplicateTagsArguments, Locale.ENGLISH).isError());
        assertTrue(mcpToolRegistry.callToolByEndpointAndNameAndArguments(McpEndpointType.TASKS, "update_task", emptyChangesArguments, Locale.ENGLISH).isError());
        verifyNoInteractions(skillService, taskService);
    }

    @Test
    void rejectsOutOfRangePaginationBeforeCallingServices() {
        // Verifies page size limits advertised in schemas are also enforced at runtime.
        var arguments = new ObjectMapper().createObjectNode().put("profileId", "profile-1").put("page", 0).put("pageSize", 101);

        var toolResultRecord = mcpToolRegistry.callToolByEndpointAndNameAndArguments(McpEndpointType.CONTEXT, "list_profiles", arguments, Locale.ENGLISH);

        assertTrue(toolResultRecord.isError());
        verifyNoInteractions(profileService);
    }

    private McpToolDefinitionRecord findToolByName(List<McpToolDefinitionRecord> toolDefinitionRecordList, String toolName) {
        return toolDefinitionRecordList.stream().filter(toolDefinitionRecord -> toolDefinitionRecord.name().equals(toolName)).findFirst().orElseThrow();
    }

    private Map<?, ?> successSchema(McpToolDefinitionRecord toolDefinitionRecord) {
        var schemaList = (List<?>) toolDefinitionRecord.outputSchema().get("oneOf");
        var firstSchema = assertInstanceOf(Map.class, schemaList.getFirst());
        return firstSchema;
    }

    private Map<?, ?> objectValue(Object value) {
        return assertInstanceOf(Map.class, value);
    }
}
