package io.agentmemoryhub.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentmemoryhub.api.common.McpEndpointType;
import io.agentmemoryhub.api.model.McpDeleteResultRecord;
import io.agentmemoryhub.api.model.McpTextContentRecord;
import io.agentmemoryhub.api.model.McpToolDefinitionRecord;
import io.agentmemoryhub.api.model.McpToolResultRecord;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.ContextResourceRecord;
import io.agentmemoryhub.core.model.record.ProjectContextRecord;
import io.agentmemoryhub.core.model.record.MemoryInputRecord;
import io.agentmemoryhub.core.model.record.MemoryPageRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalInputRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalPageRecord;
import io.agentmemoryhub.core.model.record.MemoryProposalRecord;
import io.agentmemoryhub.core.model.record.MemoryRecord;
import io.agentmemoryhub.core.model.record.ProfileInputRecord;
import io.agentmemoryhub.core.model.record.ProfilePageRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.ProjectInputRecord;
import io.agentmemoryhub.core.model.record.ProjectPageRecord;
import io.agentmemoryhub.core.model.record.ProjectRecord;
import io.agentmemoryhub.core.model.record.ProjectSettingsRecord;
import io.agentmemoryhub.core.model.record.SkillInputRecord;
import io.agentmemoryhub.core.model.record.SkillPageRecord;
import io.agentmemoryhub.core.model.record.SkillRecord;
import io.agentmemoryhub.core.model.record.TaskBlockInputRecord;
import io.agentmemoryhub.core.model.record.TaskBlockPageRecord;
import io.agentmemoryhub.core.model.record.TaskBlockRecord;
import io.agentmemoryhub.core.model.record.TaskAnalysisRecord;
import io.agentmemoryhub.core.model.record.TaskContextRecord;
import io.agentmemoryhub.core.model.record.TaskContinuityRecord;
import io.agentmemoryhub.core.model.record.TaskCreationResultRecord;
import io.agentmemoryhub.core.model.record.TaskInputRecord;
import io.agentmemoryhub.core.model.record.TaskPageRecord;
import io.agentmemoryhub.core.model.record.TaskRecord;
import io.agentmemoryhub.core.model.record.WorkspaceReferenceRecord;
import io.agentmemoryhub.core.model.record.WorkflowInputRecord;
import io.agentmemoryhub.core.model.record.WorkflowPageRecord;
import io.agentmemoryhub.core.model.record.WorkflowRecord;
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
import io.agentmemoryhub.core.utils.CollectionUtils;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class McpToolRegistryImpl implements McpToolRegistry {
    private final ContextService contextService;
    private final ProjectService projectService;
    private final ProjectSettingsService projectSettingsService;
    private final ProfileService profileService;
    private final SkillService skillService;
    private final WorkflowService workflowService;
    private final MemoryService memoryService;
    private final MemoryProposalService memoryProposalService;
    private final TaskService taskService;
    private final TaskBlockService taskBlockService;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    @Override
    public List<McpToolDefinitionRecord> listToolsByEndpoint(McpEndpointType mcpEndpointType, Locale locale) {
        var toolDefinitionRecordList = mcpEndpointType == McpEndpointType.CONTEXT ? createContextToolList() : createTaskToolList();
        var localizedToolDefinitionRecordList = new ArrayList<McpToolDefinitionRecord>();

        for (var toolDefinitionRecord : toolDefinitionRecordList) {
            var descriptionKey = "mcp.tool." + toolDefinitionRecord.name() + ".description";
            var description = messageSource.getMessage(descriptionKey, null, toolDefinitionRecord.description(), locale);
            localizedToolDefinitionRecordList.add(new McpToolDefinitionRecord(toolDefinitionRecord.name(), description, toolDefinitionRecord.inputSchema(), toolDefinitionRecord.outputSchema(), toolDefinitionRecord.annotations()));
        }

        return List.copyOf(localizedToolDefinitionRecordList);
    }

    @Override
    public McpToolResultRecord callToolByEndpointAndNameAndArguments(McpEndpointType mcpEndpointType, String toolName, JsonNode arguments, Locale locale) {
        try {
            var toolDefinitionRecord = findToolByEndpointAndName(mcpEndpointType, toolName, locale);
            validateInputArguments(arguments, toolDefinitionRecord.inputSchema());
            var data = mcpEndpointType == McpEndpointType.CONTEXT ? callContextTool(toolName, arguments) : callTaskTool(toolName, arguments, locale);
            return createSuccessResult(data);
        } catch (HubException hubException) {
            return createErrorResult(hubException.getCode(), messageSource.getMessage(hubException.getMessageKey(), hubException.getMessageArgs().toArray(), hubException.getMessageKey(), locale), hubException.getDetails());
        } catch (IllegalArgumentException exception) {
            var errorMessage = messageSource.getMessage("mcp.error.invalidToolArguments", null, "The tool arguments are invalid.", locale);
            return createErrorResult("INVALID_ARGUMENT", errorMessage, List.of());
        }
    }

    private List<McpToolDefinitionRecord> createContextToolList() {
        var toolDefinitionRecordList = new ArrayList<McpToolDefinitionRecord>();
        toolDefinitionRecordList.add(createTool("get_project_settings", "Returns the settings that govern this project's context and task tools.", properties("projectId", stringProperty()), List.of("projectId"), true, false));
        toolDefinitionRecordList.add(createTool("get_active_profile", "Returns the currently selected enabled profile.", properties("projectId", stringProperty()), List.of("projectId"), true, false));
        toolDefinitionRecordList.add(createTool("get_project_context", "Returns permanent context selected for the prompt.", properties("projectId", stringProperty(), "prompt", stringProperty()), List.of("projectId"), true, false));
        toolDefinitionRecordList.add(createTool("get_context_resource", "Returns the full text of one workflow, skill, or memory.", properties("projectId", stringProperty(), "resourceType", enumStringProperty("WORKFLOW", "SKILL", "MEMORY"), "resourceId", stringProperty()), List.of("projectId", "resourceType", "resourceId"), true, false));

        toolDefinitionRecordList.add(createTool("list_projects", "Lists projects available in this local Hub.", paginatedProperties(), List.of(), true, false));
        toolDefinitionRecordList.add(createTool("get_project", "Returns one project by its Hub identifier.", properties("projectId", stringProperty()), List.of("projectId"), true, false));
        toolDefinitionRecordList.add(createTool("create_project", "Creates a project owned by the specified existing user.", properties("name", stringProperty(), "description", stringProperty(), "enabled", booleanProperty(), "ownerUserId", stringProperty()), List.of("name", "ownerUserId"), false, false));
        toolDefinitionRecordList.add(createTool("update_project", "Applies the supplied project changes.", properties("projectId", stringProperty(), "changes", objectProperty(properties("name", stringProperty(), "description", stringProperty(), "enabled", booleanProperty())), "expectedVersion", integerProperty(1, null)), List.of("projectId", "changes"), false, false));
        toolDefinitionRecordList.add(createTool("delete_project", "Deletes a project and its dependent data after confirmation.", properties("projectId", stringProperty(), "confirm", booleanProperty()), List.of("projectId", "confirm"), false, true));

        toolDefinitionRecordList.add(createTool("list_profiles", "Lists profiles for a project.", paginatedProperties("projectId"), List.of("projectId"), true, false));
        toolDefinitionRecordList.add(createTool("get_profile", "Returns one profile within a project.", properties("projectId", stringProperty(), "profileId", stringProperty()), List.of("projectId", "profileId"), true, false));
        toolDefinitionRecordList.add(createTool("create_profile", "Creates a profile in a project.", properties("projectId", stringProperty(), "name", stringProperty(), "description", stringProperty(), "enabled", booleanProperty()), List.of("projectId", "name"), false, false));
        toolDefinitionRecordList.add(createTool("update_profile", "Applies the supplied profile changes.", properties("projectId", stringProperty(), "profileId", stringProperty(), "changes", objectProperty(properties("name", stringProperty(), "description", stringProperty(), "enabled", booleanProperty())), "expectedVersion", integerProperty(1, null)), List.of("projectId", "profileId", "changes"), false, false));
        toolDefinitionRecordList.add(createTool("delete_profile", "Deletes a profile and its dependent data after confirmation.", properties("projectId", stringProperty(), "profileId", stringProperty(), "confirm", booleanProperty()), List.of("projectId", "profileId", "confirm"), false, true));
        toolDefinitionRecordList.add(createTool("select_active_profile", "Selects an enabled profile for the project.", properties("projectId", stringProperty(), "profileId", stringProperty()), List.of("projectId", "profileId"), false, false));

        toolDefinitionRecordList.add(createTool("list_skills", "Lists skills belonging to a profile.", paginatedProperties("profileId"), List.of("profileId"), true, false));
        toolDefinitionRecordList.add(createTool("get_skill", "Returns one skill belonging to a profile.", properties("profileId", stringProperty(), "skillId", stringProperty()), List.of("profileId", "skillId"), true, false));
        toolDefinitionRecordList.add(createTool("create_skill", "Creates a skill in the specified profile.", properties("profileId", stringProperty(), "name", stringProperty(), "description", stringProperty(), "instructions", stringProperty(), "tags", stringArrayProperty(), "applicationMode", applicationModeProperty(), "enabled", booleanProperty(), "requestId", stringProperty()), List.of("profileId", "name", "instructions"), false, false));
        toolDefinitionRecordList.add(createTool("update_skill", "Applies the supplied skill changes.", properties("profileId", stringProperty(), "skillId", stringProperty(), "changes", objectProperty(properties("name", stringProperty(), "description", stringProperty(), "instructions", stringProperty(), "tags", stringArrayProperty(), "applicationMode", applicationModeProperty(), "enabled", booleanProperty())), "expectedVersion", integerProperty(1, null)), List.of("profileId", "skillId", "changes"), false, false));
        toolDefinitionRecordList.add(createTool("delete_skill", "Deletes a skill after confirmation.", properties("profileId", stringProperty(), "skillId", stringProperty(), "confirm", booleanProperty()), List.of("profileId", "skillId", "confirm"), false, true));

        toolDefinitionRecordList.add(createTool("list_workflows", "Lists workflows belonging to a profile.", paginatedProperties("profileId"), List.of("profileId"), true, false));
        toolDefinitionRecordList.add(createTool("get_workflow", "Returns one workflow belonging to a profile.", properties("profileId", stringProperty(), "workflowId", stringProperty()), List.of("profileId", "workflowId"), true, false));
        toolDefinitionRecordList.add(createTool("create_workflow", "Creates a workflow in the specified profile.", properties("profileId", stringProperty(), "name", stringProperty(), "description", stringProperty(), "instructions", stringProperty(), "enabled", booleanProperty(), "requestId", stringProperty()), List.of("profileId", "name", "instructions"), false, false));
        toolDefinitionRecordList.add(createTool("update_workflow", "Applies the supplied workflow changes.", properties("profileId", stringProperty(), "workflowId", stringProperty(), "changes", objectProperty(properties("name", stringProperty(), "description", stringProperty(), "instructions", stringProperty(), "enabled", booleanProperty())), "expectedVersion", integerProperty(1, null)), List.of("profileId", "workflowId", "changes"), false, false));
        toolDefinitionRecordList.add(createTool("delete_workflow", "Deletes a workflow after confirmation.", properties("profileId", stringProperty(), "workflowId", stringProperty(), "confirm", booleanProperty()), List.of("profileId", "workflowId", "confirm"), false, true));

        toolDefinitionRecordList.add(createTool("list_memories", "Lists memories belonging to a profile.", paginatedProperties("profileId"), List.of("profileId"), true, false));
        toolDefinitionRecordList.add(createTool("get_memory", "Returns one memory belonging to a profile.", properties("profileId", stringProperty(), "memoryId", stringProperty()), List.of("profileId", "memoryId"), true, false));
        toolDefinitionRecordList.add(createTool("create_memory", "Creates a memory when explicitly requested.", properties("profileId", stringProperty(), "title", stringProperty(), "content", stringProperty(), "tags", stringArrayProperty(), "applicationMode", applicationModeProperty(), "enabled", booleanProperty(), "requestId", stringProperty()), List.of("profileId", "title", "content", "applicationMode"), false, false));
        toolDefinitionRecordList.add(createTool("update_memory", "Applies the supplied memory changes.", properties("profileId", stringProperty(), "memoryId", stringProperty(), "changes", objectProperty(properties("title", stringProperty(), "content", stringProperty(), "tags", stringArrayProperty(), "applicationMode", applicationModeProperty(), "enabled", booleanProperty())), "expectedVersion", integerProperty(1, null)), List.of("profileId", "memoryId", "changes"), false, false));
        toolDefinitionRecordList.add(createTool("delete_memory", "Deletes a memory after confirmation.", properties("profileId", stringProperty(), "memoryId", stringProperty(), "confirm", booleanProperty()), List.of("profileId", "memoryId", "confirm"), false, true));

        toolDefinitionRecordList.add(createTool("propose_memory", "Creates a pending memory proposal linked to an existing task.", properties("taskId", stringProperty(), "proposedContent", stringProperty(), "reason", stringProperty(), "tags", stringArrayProperty(), "requestId", stringProperty()), List.of("taskId", "proposedContent", "reason"), false, false));
        toolDefinitionRecordList.add(createTool("list_memory_proposals", "Lists proposals linked to a task.", paginatedProperties("taskId"), List.of("taskId"), true, false));
        toolDefinitionRecordList.add(createTool("get_memory_proposal", "Returns one memory proposal.", properties("proposalId", stringProperty()), List.of("proposalId"), true, false));
        toolDefinitionRecordList.add(createTool("update_memory_proposal", "Updates a pending memory proposal.", properties("proposalId", stringProperty(), "changes", objectProperty(properties("proposedContent", stringProperty(), "reason", stringProperty(), "tags", stringArrayProperty())), "expectedVersion", integerProperty(1, null)), List.of("proposalId", "changes"), false, false));
        toolDefinitionRecordList.add(createTool("delete_memory_proposal", "Deletes a proposal without changing an approved memory.", properties("proposalId", stringProperty(), "confirm", booleanProperty()), List.of("proposalId", "confirm"), false, true));

        return List.copyOf(toolDefinitionRecordList);
    }

    private List<McpToolDefinitionRecord> createTaskToolList() {
        var toolDefinitionRecordList = new ArrayList<McpToolDefinitionRecord>();
        toolDefinitionRecordList.add(createTool("get_project_settings", "Returns the settings that govern this project's context and task tools.", properties("projectId", stringProperty()), List.of("projectId"), true, false));
        toolDefinitionRecordList.add(createTool("list_tasks", "Lists tasks belonging to a profile.", paginatedProperties("profileId"), List.of("profileId"), true, false));
        toolDefinitionRecordList.add(createTool("get_task", "Returns one task by its Hub identifier.", properties("taskId", stringProperty()), List.of("taskId"), true, false));
        toolDefinitionRecordList.add(createTool("create_task", "Creates a task in the selected profile; it never writes local files.", properties("profileId", stringProperty(), "title", stringProperty(), "description", stringProperty(), "summary", stringProperty(), "requestId", stringProperty()), List.of("profileId", "title", "description"), false, false));
        toolDefinitionRecordList.add(createTool("update_task", "Applies the supplied task changes.", properties("taskId", stringProperty(), "changes", objectProperty(properties("title", stringProperty(), "description", stringProperty(), "summary", stringProperty())), "expectedVersion", integerProperty(1, null)), List.of("taskId", "changes"), false, false));
        toolDefinitionRecordList.add(createTool("delete_task", "Deletes a task and its blocks and proposals after confirmation.", properties("taskId", stringProperty(), "confirm", booleanProperty()), List.of("taskId", "confirm"), false, true));
        toolDefinitionRecordList.add(createTool("get_task_context", "Returns the selected task and relevant blocks within its project.", properties("projectId", stringProperty(), "taskId", stringProperty(), "prompt", stringProperty()), List.of("projectId", "taskId"), true, false));
        toolDefinitionRecordList.add(createTool("list_task_blocks", "Lists blocks belonging to a task.", paginatedProperties("taskId"), List.of("taskId"), true, false));
        toolDefinitionRecordList.add(createTool("get_task_block", "Returns one complete task block.", properties("blockId", stringProperty()), List.of("blockId"), true, false));
        toolDefinitionRecordList.add(createTool("create_task_block", "Creates a free-text block for a task.", properties("taskId", stringProperty(), "content", stringProperty(), "requestId", stringProperty()), List.of("taskId", "content"), false, false));
        toolDefinitionRecordList.add(createTool("update_task_block", "Replaces the content of a task block.", properties("blockId", stringProperty(), "content", stringProperty(), "expectedVersion", integerProperty(1, null)), List.of("blockId", "content"), false, false));
        toolDefinitionRecordList.add(createTool("delete_task_block", "Deletes a task block after confirmation.", properties("blockId", stringProperty(), "confirm", booleanProperty()), List.of("blockId", "confirm"), false, true));
        toolDefinitionRecordList.add(createTool("get_workspace_reference_guidance", "Returns data and instructions for a Codex-managed local workspace reference.", properties("projectId", stringProperty(), "taskId", stringProperty()), List.of("projectId"), true, false));
        toolDefinitionRecordList.add(createTool("analyze_task_prompt", "Classifies a prompt without creating or changing a task.", properties("projectId", stringProperty(), "prompt", stringProperty(), "taskId", stringProperty()), List.of("projectId", "prompt"), true, false));
        toolDefinitionRecordList.add(createTool("inspect_task_continuity", "Checks whether a task belongs to the active profile and counts its blocks.", properties("projectId", stringProperty(), "taskId", stringProperty()), List.of("projectId"), true, false));
        return List.copyOf(toolDefinitionRecordList);
    }

    private McpToolDefinitionRecord createTool(String name, String description, Map<String, Object> inputProperties, List<String> requiredFields, boolean readOnly, boolean destructive) {
        var inputSchema = Map.<String, Object>of("type", "object", "properties", inputProperties, "required", requiredFields, "additionalProperties", false);
        var outputSchema = outputSchemaByToolName(name);
        var annotations = Map.<String, Object>of("readOnlyHint", readOnly, "destructiveHint", destructive, "openWorldHint", false);
        return new McpToolDefinitionRecord(name, description, inputSchema, outputSchema, annotations);
    }

    private Map<String, Object> properties(Object... propertyEntries) {
        return CollectionUtils.mapOf(propertyEntries);
    }

    private Map<String, Object> paginatedProperties(String... identityFields) {
        var properties = new LinkedHashMap<String, Object>();

        for (var identityField : identityFields) {
            properties.put(identityField, stringProperty());
        }

        properties.put("page", integerProperty(0, null));
        properties.put("pageSize", integerProperty(1, 100));
        return Map.copyOf(properties);
    }

    private Map<String, Object> stringProperty() {
        return Map.of("type", "string", "minLength", 1);
    }

    private Map<String, Object> booleanProperty() {
        return Map.of("type", "boolean");
    }

    private Map<String, Object> integerProperty(Integer minimum, Integer maximum) {
        var propertySchema = new LinkedHashMap<String, Object>();
        propertySchema.put("type", "integer");

        if (Objects.nonNull(minimum)) {
            propertySchema.put("minimum", minimum);
        }

        if (Objects.nonNull(maximum)) {
            propertySchema.put("maximum", maximum);
        }

        return Map.copyOf(propertySchema);
    }

    private Map<String, Object> stringArrayProperty() {
        return Map.of("type", "array", "items", stringProperty(), "maxItems", 100, "uniqueItems", true);
    }

    private Map<String, Object> applicationModeProperty() {
        return enumStringProperty("ALWAYS", "CONTEXTUAL");
    }

    private Map<String, Object> enumStringProperty(String... allowedValues) {
        return Map.of("type", "string", "enum", List.of(allowedValues));
    }

    private Map<String, Object> objectProperty(Map<String, Object> nestedProperties) {
        return Map.of("type", "object", "properties", nestedProperties, "minProperties", 1, "additionalProperties", false);
    }

    private Map<String, Object> outputSchemaByToolName(String toolName) {
        var dataType = switch (toolName) {
            case "get_active_profile", "get_profile", "create_profile", "update_profile", "select_active_profile" -> ProfileRecord.class;
            case "get_project_context" -> ProjectContextRecord.class;
            case "get_project_settings" -> ProjectSettingsRecord.class;
            case "get_context_resource" -> ContextResourceRecord.class;
            case "get_project", "create_project", "update_project" -> ProjectRecord.class;
            case "list_projects" -> ProjectPageRecord.class;
            case "list_profiles" -> ProfilePageRecord.class;
            case "get_skill", "create_skill", "update_skill" -> SkillRecord.class;
            case "list_skills" -> SkillPageRecord.class;
            case "get_workflow", "create_workflow", "update_workflow" -> WorkflowRecord.class;
            case "list_workflows" -> WorkflowPageRecord.class;
            case "get_memory", "create_memory", "update_memory" -> MemoryRecord.class;
            case "list_memories" -> MemoryPageRecord.class;
            case "propose_memory", "get_memory_proposal", "update_memory_proposal" -> MemoryProposalRecord.class;
            case "list_memory_proposals" -> MemoryProposalPageRecord.class;
            case "create_task" -> TaskCreationResultRecord.class;
            case "get_task", "update_task" -> TaskRecord.class;
            case "get_task_context" -> TaskContextRecord.class;
            case "list_tasks" -> TaskPageRecord.class;
            case "get_task_block", "create_task_block", "update_task_block" -> TaskBlockRecord.class;
            case "list_task_blocks" -> TaskBlockPageRecord.class;
            case "get_workspace_reference_guidance" -> WorkspaceReferenceRecord.class;
            case "analyze_task_prompt" -> TaskAnalysisRecord.class;
            case "inspect_task_continuity" -> TaskContinuityRecord.class;
            default -> McpDeleteResultRecord.class;
        };

        var dataSchema = recordSchema(dataType);

        if ("get_active_profile".equals(toolName)) {
            dataSchema = Map.of("anyOf", List.of(dataSchema, Map.of("type", "null")));
        }

        var successSchema = Map.<String, Object>of(
                "type", "object",
                "properties", Map.of(
                        "success", Map.of("type", "boolean", "enum", List.of(true)),
                        "data", dataSchema
                ),
                "required", List.of("success", "data"),
                "additionalProperties", false
        );
        var errorSchema = Map.<String, Object>of(
                "type", "object",
                "properties", Map.of(
                        "success", Map.of("type", "boolean", "enum", List.of(false)),
                        "error", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "code", Map.of("type", "string"),
                                        "message", Map.of("type", "string"),
                                        "details", Map.of("type", "array", "items", Map.of("type", "string"))
                                ),
                                "required", List.of("code", "message", "details"),
                                "additionalProperties", false
                        )
                ),
                "required", List.of("success", "error"),
                "additionalProperties", false
        );
        return Map.of("oneOf", List.of(successSchema, errorSchema));
    }

    private Map<String, Object> recordSchema(Class<?> recordType) {
        var properties = new LinkedHashMap<String, Object>();
        var requiredFields = new ArrayList<String>();

        for (var recordComponent : recordType.getRecordComponents()) {
            var componentSchema = schemaForType(recordComponent.getGenericType());

            if (!recordComponent.getType().isPrimitive()) {
                componentSchema = Map.of("anyOf", List.of(componentSchema, Map.of("type", "null")));
            }

            properties.put(recordComponent.getName(), componentSchema);
            requiredFields.add(recordComponent.getName());
        }

        return Map.of("type", "object", "properties", Map.copyOf(properties), "required", List.copyOf(requiredFields), "additionalProperties", false);
    }

    private Map<String, Object> schemaForType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            var rawType = parameterizedType.getRawType();

            if (rawType instanceof Class<?> rawClass && List.class.isAssignableFrom(rawClass)) {
                return Map.of("type", "array", "items", schemaForType(parameterizedType.getActualTypeArguments()[0]));
            }

            if (rawType instanceof Class<?> rawClass && Map.class.isAssignableFrom(rawClass)) {
                return Map.of("type", "object", "additionalProperties", true);
            }

            return Map.of("type", "object", "additionalProperties", true);
        }

        if (!(type instanceof Class<?> typeClass)) {
            return Map.of("type", "object", "additionalProperties", true);
        }

        if (typeClass.isRecord()) {
            return recordSchema(typeClass);
        }

        if (typeClass.isEnum()) {
            var enumValues = java.util.Arrays.stream(typeClass.getEnumConstants()).map(Object::toString).toList();
            return Map.of("type", "string", "enum", enumValues);
        }

        if (typeClass == String.class || TemporalAccessor.class.isAssignableFrom(typeClass)) {
            var stringSchema = new LinkedHashMap<String, Object>();
            stringSchema.put("type", "string");

            if (TemporalAccessor.class.isAssignableFrom(typeClass)) {
                stringSchema.put("format", "date-time");
            }

            return Map.copyOf(stringSchema);
        }

        if (typeClass == Boolean.class || typeClass == boolean.class) {
            return Map.of("type", "boolean");
        }

        if (typeClass == Integer.class || typeClass == int.class || typeClass == Long.class || typeClass == long.class || typeClass == Short.class || typeClass == short.class || typeClass == Byte.class || typeClass == byte.class || typeClass == BigInteger.class) {
            return Map.of("type", "integer");
        }

        if (Number.class.isAssignableFrom(typeClass) || typeClass == double.class || typeClass == float.class || typeClass == BigDecimal.class) {
            return Map.of("type", "number");
        }

        return Map.of("type", "object", "additionalProperties", true);
    }

    private McpToolDefinitionRecord findToolByEndpointAndName(McpEndpointType mcpEndpointType, String toolName, Locale locale) {
        return listToolsByEndpoint(mcpEndpointType, locale).stream().filter(toolDefinitionRecord -> toolDefinitionRecord.name().equals(toolName)).findFirst().orElseThrow(() -> new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name"));
    }

    private void validateInputArguments(JsonNode arguments, Map<String, Object> inputSchema) {
        validateSchemaNode(arguments, inputSchema, "arguments");
    }

    private void validateSchemaNode(JsonNode valueNode, Map<String, Object> schema, String path) {
        var expectedType = Objects.toString(schema.get("type"), "");

        if ("object".equals(expectedType)) {
            validateObjectSchema(valueNode, schema, path);
            return;
        }

        if ("string".equals(expectedType)) {
            validateStringSchema(valueNode, schema, path);
            return;
        }

        if ("boolean".equals(expectedType) && !valueNode.isBoolean()) {
            throw invalidArgument(path);
        }

        if ("integer".equals(expectedType) && !valueNode.isIntegralNumber()) {
            throw invalidArgument(path);
        }

        if ("integer".equals(expectedType)) {
            validateIntegerBounds(valueNode, schema, path);
        }

        if ("array".equals(expectedType)) {
            validateArraySchema(valueNode, schema, path);
        }
    }

    private void validateObjectSchema(JsonNode valueNode, Map<String, Object> schema, String path) {
        if (Objects.isNull(valueNode) || !valueNode.isObject()) {
            throw invalidArgument(path);
        }
        var propertySchemaMap = castSchemaMap(schema.get("properties"));

        for (var fieldName : schemaList(schema.get("required"))) {
            if (!valueNode.has(fieldName) || valueNode.get(fieldName).isNull()) {
                throw invalidArgument(path + "." + fieldName);
            }
        }

        if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            valueNode.fieldNames().forEachRemaining(fieldName -> {
                if (!propertySchemaMap.containsKey(fieldName)) {
                    throw invalidArgument(path + "." + fieldName);
                }
            });
        }

        var minimumProperties = schema.get("minProperties") instanceof Integer value ? value : 0;

        if (valueNode.size() < minimumProperties) {
            throw invalidArgument(path);
        }

        valueNode.fields().forEachRemaining(fieldEntry -> {
            var nestedSchema = propertySchemaMap.get(fieldEntry.getKey());

            if (nestedSchema instanceof Map<?, ?> nestedSchemaMap) {
                validateSchemaNode(fieldEntry.getValue(), castSchemaMap(nestedSchemaMap), path + "." + fieldEntry.getKey());
            }
        });
    }

    private void validateStringSchema(JsonNode valueNode, Map<String, Object> schema, String path) {
        if (Objects.isNull(valueNode) || !valueNode.isTextual()) {
            throw invalidArgument(path);
        }

        var minimumLength = schema.get("minLength") instanceof Integer value ? value : 0;

        if (valueNode.asText().length() < minimumLength || (minimumLength > 0 && valueNode.asText().isBlank())) {
            throw invalidArgument(path);
        }

        var allowedValue = schema.get("enum");

        if (allowedValue instanceof List<?> allowedValueList && !allowedValueList.contains(valueNode.asText())) {
            throw invalidArgument(path);
        }
    }

    private void validateArraySchema(JsonNode valueNode, Map<String, Object> schema, String path) {
        if (Objects.isNull(valueNode) || !valueNode.isArray()) {
            throw invalidArgument(path);
        }

        var itemSchema = schema.get("items");

        if (itemSchema instanceof Map<?, ?> itemSchemaMap) {
            for (int index = 0; index < valueNode.size(); index++) {
                validateSchemaNode(valueNode.get(index), castSchemaMap(itemSchemaMap), path + "[" + index + "]");
            }
        }

        var maximumItems = schema.get("maxItems") instanceof Integer value ? value : null;

        if (Objects.nonNull(maximumItems) && valueNode.size() > maximumItems) {
            throw invalidArgument(path);
        }

        if (Boolean.TRUE.equals(schema.get("uniqueItems"))) {
            var seenValueSet = new HashSet<JsonNode>();

            for (var itemNode : valueNode) {
                if (!seenValueSet.add(itemNode)) {
                    throw invalidArgument(path);
                }
            }
        }
    }

    private void validateIntegerBounds(JsonNode valueNode, Map<String, Object> schema, String path) {
        var integerValue = valueNode.bigIntegerValue();
        var minimum = schema.get("minimum") instanceof Number number ? BigInteger.valueOf(number.longValue()) : null;
        var maximum = schema.get("maximum") instanceof Number number ? BigInteger.valueOf(number.longValue()) : null;

        if ((Objects.nonNull(minimum) && integerValue.compareTo(minimum) < 0) || (Objects.nonNull(maximum) && integerValue.compareTo(maximum) > 0)) {
            throw invalidArgument(path);
        }
    }

    private List<String> schemaList(Object value) {
        if (value instanceof List<?> valueList) {
            return valueList.stream().map(Object::toString).toList();
        }

        return List.of();
    }

    private Map<String, Object> castSchemaMap(Object schemaObject) {
        if (!(schemaObject instanceof Map<?, ?> schemaMap)) {
            return Map.of();
        }

        var convertedSchemaMap = new LinkedHashMap<String, Object>();
        schemaMap.forEach((key, value) -> convertedSchemaMap.put(key.toString(), value));
        return convertedSchemaMap;
    }

    private Object callContextTool(String toolName, JsonNode arguments) {
        return switch (toolName) {
            case "get_project_settings" -> projectSettingsService.getProjectSettingsByProjectId(requiredString(arguments, "projectId"));
            case "get_active_profile" -> contextService.getActiveProfileByProjectId(requiredString(arguments, "projectId"));
            case "get_project_context" -> contextService.getProjectContextByProjectIdAndPrompt(requiredString(arguments, "projectId"), optionalString(arguments, "prompt"));
            case "get_context_resource" -> contextService.getContextResourceByProjectIdAndTypeAndResourceId(requiredString(arguments, "projectId"), requiredString(arguments, "resourceType"), requiredString(arguments, "resourceId"));
            case "list_projects" -> projectService.listProjects(optionalInteger(arguments, "page", 0), optionalInteger(arguments, "pageSize", 20));
            case "get_project" -> projectService.getProjectById(requiredString(arguments, "projectId"));
            case "create_project" -> projectService.createProjectByInput(convertInput(arguments, ProjectInputRecord.class, "name", "description", "enabled", "ownerUserId"));
            case "update_project" -> projectService.updateProjectById(requiredString(arguments, "projectId"), convertChanges(arguments, ProjectInputRecord.class, "name", "description", "enabled"), optionalVersion(arguments));
            case "delete_project" -> deleteProject(arguments);
            case "list_profiles" -> profileService.listProfilesByProjectId(requiredString(arguments, "projectId"), optionalInteger(arguments, "page", 0), optionalInteger(arguments, "pageSize", 20));
            case "get_profile" -> profileService.getProfileByIdAndProjectId(requiredString(arguments, "profileId"), requiredString(arguments, "projectId"));
            case "create_profile" -> profileService.createProfileByProjectIdAndInput(requiredString(arguments, "projectId"), convertInput(arguments, ProfileInputRecord.class, "name", "description", "enabled"));
            case "update_profile" -> profileService.updateProfileByIdAndProjectId(requiredString(arguments, "profileId"), requiredString(arguments, "projectId"), convertChanges(arguments, ProfileInputRecord.class, "name", "description", "enabled"), optionalVersion(arguments));
            case "delete_profile" -> deleteProfile(arguments);
            case "select_active_profile" -> projectService.selectActiveProfileByProjectIdAndProfileId(requiredString(arguments, "projectId"), requiredString(arguments, "profileId"));
            case "list_skills" -> skillService.listSkillsByProfileId(requiredString(arguments, "profileId"), optionalInteger(arguments, "page", 0), optionalInteger(arguments, "pageSize", 20));
            case "get_skill" -> skillService.getSkillByIdAndProfileId(requiredString(arguments, "skillId"), requiredString(arguments, "profileId"));
            case "create_skill" -> skillService.createSkillByProfileIdAndInput(requiredString(arguments, "profileId"), convertInput(arguments, SkillInputRecord.class, "name", "description", "instructions", "tags", "applicationMode", "enabled"), optionalString(arguments, "requestId"));
            case "update_skill" -> skillService.updateSkillByIdAndProfileId(requiredString(arguments, "skillId"), requiredString(arguments, "profileId"), convertChanges(arguments, SkillInputRecord.class, "name", "description", "instructions", "tags", "applicationMode", "enabled"), optionalVersion(arguments));
            case "delete_skill" -> deleteSkill(arguments);
            case "list_workflows" -> workflowService.listWorkflowsByProfileId(requiredString(arguments, "profileId"), optionalInteger(arguments, "page", 0), optionalInteger(arguments, "pageSize", 20));
            case "get_workflow" -> workflowService.getWorkflowByIdAndProfileId(requiredString(arguments, "workflowId"), requiredString(arguments, "profileId"));
            case "create_workflow" -> workflowService.createWorkflowByProfileIdAndInput(requiredString(arguments, "profileId"), convertInput(arguments, WorkflowInputRecord.class, "name", "description", "instructions", "enabled"), optionalString(arguments, "requestId"));
            case "update_workflow" -> workflowService.updateWorkflowByIdAndProfileId(requiredString(arguments, "workflowId"), requiredString(arguments, "profileId"), convertChanges(arguments, WorkflowInputRecord.class, "name", "description", "instructions", "enabled"), optionalVersion(arguments));
            case "delete_workflow" -> deleteWorkflow(arguments);
            case "list_memories" -> memoryService.listMemoriesByProfileId(requiredString(arguments, "profileId"), optionalInteger(arguments, "page", 0), optionalInteger(arguments, "pageSize", 20));
            case "get_memory" -> memoryService.getMemoryByIdAndProfileId(requiredString(arguments, "memoryId"), requiredString(arguments, "profileId"));
            case "create_memory" -> memoryService.createMemoryByProfileIdAndInput(requiredString(arguments, "profileId"), convertInput(arguments, MemoryInputRecord.class, "title", "content", "tags", "applicationMode", "enabled"), optionalString(arguments, "requestId"));
            case "update_memory" -> memoryService.updateMemoryByIdAndProfileId(requiredString(arguments, "memoryId"), requiredString(arguments, "profileId"), convertChanges(arguments, MemoryInputRecord.class, "title", "content", "tags", "applicationMode", "enabled"), optionalVersion(arguments));
            case "delete_memory" -> deleteMemory(arguments);
            case "propose_memory" -> memoryProposalService.createMemoryProposalByTaskIdAndInput(requiredString(arguments, "taskId"), convertInput(arguments, MemoryProposalInputRecord.class, "proposedContent", "reason", "tags"), optionalString(arguments, "requestId"));
            case "list_memory_proposals" -> memoryProposalService.listMemoryProposalsByTaskId(requiredString(arguments, "taskId"), optionalInteger(arguments, "page", 0), optionalInteger(arguments, "pageSize", 20));
            case "get_memory_proposal" -> memoryProposalService.getMemoryProposalById(requiredString(arguments, "proposalId"));
            case "update_memory_proposal" -> memoryProposalService.updateMemoryProposalById(requiredString(arguments, "proposalId"), convertChanges(arguments, MemoryProposalInputRecord.class, "proposedContent", "reason", "tags"), optionalVersion(arguments));
            case "delete_memory_proposal" -> deleteMemoryProposal(arguments);
            default -> throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        };
    }

    private Object callTaskTool(String toolName, JsonNode arguments, Locale locale) {
        return switch (toolName) {
            case "get_project_settings" -> projectSettingsService.getProjectSettingsByProjectId(requiredString(arguments, "projectId"));
            case "list_tasks" -> taskService.listTasksByProfileId(requiredString(arguments, "profileId"), optionalInteger(arguments, "page", 0), optionalInteger(arguments, "pageSize", 20));
            case "get_task" -> taskService.getTaskById(requiredString(arguments, "taskId"));
            case "create_task" -> createTask(arguments, locale);
            case "update_task" -> taskService.updateTaskById(requiredString(arguments, "taskId"), convertChanges(arguments, TaskInputRecord.class, "title", "description", "summary"), optionalVersion(arguments));
            case "delete_task" -> deleteTask(arguments);
            case "get_task_context" -> taskService.getTaskContextByProjectIdAndTaskId(requiredString(arguments, "projectId"), requiredString(arguments, "taskId"), optionalString(arguments, "prompt"));
            case "list_task_blocks" -> taskBlockService.listTaskBlocksByTaskId(requiredString(arguments, "taskId"), optionalInteger(arguments, "page", 0), optionalInteger(arguments, "pageSize", 20));
            case "get_task_block" -> taskBlockService.getTaskBlockById(requiredString(arguments, "blockId"));
            case "create_task_block" -> taskBlockService.createTaskBlockByTaskIdAndInput(requiredString(arguments, "taskId"), convertInput(arguments, TaskBlockInputRecord.class, "content"), optionalString(arguments, "requestId"));
            case "update_task_block" -> taskBlockService.updateTaskBlockById(requiredString(arguments, "blockId"), convertInput(arguments, TaskBlockInputRecord.class, "content"), optionalVersion(arguments));
            case "delete_task_block" -> deleteTaskBlock(arguments);
            case "get_workspace_reference_guidance" -> localizeWorkspaceReference(taskService.getWorkspaceReferenceGuidanceByProjectIdAndTaskId(requiredString(arguments, "projectId"), optionalString(arguments, "taskId")), locale);
            case "analyze_task_prompt" -> localizeTaskAnalysis(taskService.analyzeTaskPromptByProjectIdAndTaskId(requiredString(arguments, "projectId"), requiredString(arguments, "prompt"), optionalString(arguments, "taskId")), locale);
            case "inspect_task_continuity" -> taskService.inspectTaskContinuityByProjectIdAndTaskId(requiredString(arguments, "projectId"), optionalString(arguments, "taskId"));
            default -> throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "name");
        };
    }

    private Object deleteProject(JsonNode arguments) {
        projectService.deleteProjectById(requiredString(arguments, "projectId"), requiredBoolean(arguments, "confirm"));
        return new McpDeleteResultRecord(true);
    }

    private Object deleteProfile(JsonNode arguments) {
        profileService.deleteProfileByIdAndProjectId(requiredString(arguments, "profileId"), requiredString(arguments, "projectId"), requiredBoolean(arguments, "confirm"));
        return new McpDeleteResultRecord(true);
    }

    private Object deleteSkill(JsonNode arguments) {
        skillService.deleteSkillByIdAndProfileId(requiredString(arguments, "skillId"), requiredString(arguments, "profileId"), requiredBoolean(arguments, "confirm"));
        return new McpDeleteResultRecord(true);
    }

    private Object deleteWorkflow(JsonNode arguments) {
        workflowService.deleteWorkflowByIdAndProfileId(requiredString(arguments, "workflowId"), requiredString(arguments, "profileId"), requiredBoolean(arguments, "confirm"));
        return new McpDeleteResultRecord(true);
    }

    private Object deleteMemory(JsonNode arguments) {
        memoryService.deleteMemoryByIdAndProfileId(requiredString(arguments, "memoryId"), requiredString(arguments, "profileId"), requiredBoolean(arguments, "confirm"));
        return new McpDeleteResultRecord(true);
    }

    private Object deleteMemoryProposal(JsonNode arguments) {
        memoryProposalService.deleteMemoryProposalById(requiredString(arguments, "proposalId"), requiredBoolean(arguments, "confirm"));
        return new McpDeleteResultRecord(true);
    }

    private Object deleteTask(JsonNode arguments) {
        taskService.deleteTaskById(requiredString(arguments, "taskId"), requiredBoolean(arguments, "confirm"));
        return new McpDeleteResultRecord(true);
    }

    private Object deleteTaskBlock(JsonNode arguments) {
        taskBlockService.deleteTaskBlockById(requiredString(arguments, "blockId"), requiredBoolean(arguments, "confirm"));
        return new McpDeleteResultRecord(true);
    }

    private TaskCreationResultRecord createTask(JsonNode arguments, Locale locale) {
        var taskRecord = taskService.createTaskByProfileIdAndInput(requiredString(arguments, "profileId"), convertInput(arguments, TaskInputRecord.class, "title", "description", "summary"), optionalString(arguments, "requestId"));
        var nextSteps = List.of(
                messageSource.getMessage("mcp.taskCreation.nextStep.reference", new Object[]{taskRecord.id()}, "mcp.taskCreation.nextStep.reference", locale),
                messageSource.getMessage("mcp.taskCreation.nextStep.write", null, "mcp.taskCreation.nextStep.write", locale)
        );
        return new TaskCreationResultRecord(taskRecord, taskRecord.id(), nextSteps);
    }

    private WorkspaceReferenceRecord localizeWorkspaceReference(WorkspaceReferenceRecord workspaceReferenceRecord, Locale locale) {
        var instructionList = workspaceReferenceRecord.instructions().stream().map(messageKey -> messageSource.getMessage(messageKey, null, messageKey, locale)).toList();
        return new WorkspaceReferenceRecord(workspaceReferenceRecord.relativePath(), workspaceReferenceRecord.version(), workspaceReferenceRecord.serverUrl(), workspaceReferenceRecord.projectId(), workspaceReferenceRecord.taskId(), instructionList);
    }

    private TaskAnalysisRecord localizeTaskAnalysis(TaskAnalysisRecord taskAnalysisRecord, Locale locale) {
        var reason = messageSource.getMessage(taskAnalysisRecord.reason(), null, taskAnalysisRecord.reason(), locale);
        return new TaskAnalysisRecord(taskAnalysisRecord.mode(), taskAnalysisRecord.complexity(), taskAnalysisRecord.shouldSuggest(), taskAnalysisRecord.shouldCreateAutomatically(), reason, taskAnalysisRecord.suggestedTitle(), taskAnalysisRecord.suggestedDescription());
    }

    private <T> T convertInput(JsonNode source, Class<T> recordType, String... fieldNames) {
        return convertNodeToRecord(source, recordType, fieldNames);
    }

    private <T> T convertChanges(JsonNode arguments, Class<T> recordType, String... fieldNames) {
        var changesNode = arguments.get("changes");

        if (Objects.isNull(changesNode) || !changesNode.isObject()) {
            throw invalidArgument("arguments.changes");
        }

        return convertNodeToRecord(changesNode, recordType, fieldNames);
    }

    private <T> T convertNodeToRecord(JsonNode source, Class<T> recordType, String... fieldNames) {
        var inputNode = objectMapper.createObjectNode();

        for (var fieldName : fieldNames) {
            var valueNode = source.get(fieldName);
            if (Objects.nonNull(valueNode)) {
                inputNode.set(fieldName, valueNode);
            }
        }

        return objectMapper.convertValue(inputNode, recordType);
    }

    private String requiredString(JsonNode arguments, String fieldName) {
        var valueNode = arguments.get(fieldName);

        if (Objects.isNull(valueNode) || !valueNode.isTextual() || valueNode.asText().isBlank()) {
            throw invalidArgument("arguments." + fieldName);
        }

        return valueNode.asText();
    }

    private String optionalString(JsonNode arguments, String fieldName) {
        var valueNode = arguments.get(fieldName);

        if (Objects.isNull(valueNode) || valueNode.isNull()) {
            return null;
        }

        if (!valueNode.isTextual()) {
            throw invalidArgument("arguments." + fieldName);
        }

        return valueNode.asText();
    }

    private Boolean optionalBoolean(JsonNode arguments, String fieldName) {
        var valueNode = arguments.get(fieldName);

        if (Objects.isNull(valueNode) || valueNode.isNull()) {
            return null;
        }

        if (!valueNode.isBoolean()) {
            throw invalidArgument("arguments." + fieldName);
        }

        return valueNode.asBoolean();
    }

    private boolean requiredBoolean(JsonNode arguments, String fieldName) {
        var value = optionalBoolean(arguments, fieldName);

        if (Objects.isNull(value)) {
            throw invalidArgument("arguments." + fieldName);
        }

        return value;
    }

    private int optionalInteger(JsonNode arguments, String fieldName, int defaultValue) {
        var valueNode = arguments.get(fieldName);

        if (Objects.isNull(valueNode) || valueNode.isNull()) {
            return defaultValue;
        }

        if (!valueNode.isIntegralNumber()) {
            throw invalidArgument("arguments." + fieldName);
        }

        return valueNode.asInt();
    }

    private Long optionalVersion(JsonNode arguments) {
        var valueNode = arguments.get("expectedVersion");

        if (Objects.isNull(valueNode) || valueNode.isNull()) {
            return null;
        }

        if (!valueNode.isIntegralNumber()) {
            throw invalidArgument("arguments.expectedVersion");
        }

        return valueNode.asLong();
    }

    private HubException invalidArgument(String fieldName) {
        return new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, fieldName);
    }

    private McpToolResultRecord createSuccessResult(Object data) {
        var structuredContent = new LinkedHashMap<String, Object>();
        structuredContent.put("success", true);
        structuredContent.put("data", data);
        return new McpToolResultRecord(List.of(new McpTextContentRecord("text", serializeContent(structuredContent))), false, structuredContent);
    }

    private McpToolResultRecord createErrorResult(String code, String message, List<String> details) {
        var error = new LinkedHashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);
        error.put("details", details);
        var structuredContent = new LinkedHashMap<String, Object>();
        structuredContent.put("success", false);
        structuredContent.put("error", error);
        return new McpToolResultRecord(List.of(new McpTextContentRecord("text", serializeContent(structuredContent))), true, structuredContent);
    }

    private String serializeContent(Object content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception exception) {
            return content.toString();
        }
    }
}
