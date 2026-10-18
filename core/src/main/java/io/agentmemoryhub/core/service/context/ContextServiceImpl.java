package io.agentmemoryhub.core.service.context;

import io.agentmemoryhub.core.common.ApplicationMode;
import io.agentmemoryhub.core.common.ConnectionType;
import io.agentmemoryhub.core.converter.TagsJsonDataConverter;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Memory;
import io.agentmemoryhub.core.model.domain.Profile;
import io.agentmemoryhub.core.model.domain.Project;
import io.agentmemoryhub.core.model.domain.ProjectSettings;
import io.agentmemoryhub.core.model.domain.Skill;
import io.agentmemoryhub.core.model.domain.Workflow;
import io.agentmemoryhub.core.model.record.ContextResourceRecord;
import io.agentmemoryhub.core.model.record.ContextBudgetResultRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;
import io.agentmemoryhub.core.model.record.ProjectContextRecord;
import io.agentmemoryhub.core.repository.MemoryRepository;
import io.agentmemoryhub.core.repository.McpConnectionRepository;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.ProjectSettingsRepository;
import io.agentmemoryhub.core.repository.SkillRepository;
import io.agentmemoryhub.core.repository.WorkflowRepository;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContextServiceImpl implements ContextService {
    private final ProjectRepository projectRepository;
    private final ProfileRepository profileRepository;
    private final ProjectSettingsRepository projectSettingsRepository;
    private final WorkflowRepository workflowRepository;
    private final SkillRepository skillRepository;
    private final MemoryRepository memoryRepository;
    private final McpConnectionRepository mcpConnectionRepository;
    private final TagsJsonDataConverter tagsJsonDataConverter;

    @Override
    @Transactional(readOnly = true)
    public ProfileRecord getActiveProfileByProjectId(String projectId) {
        var project = findProjectById(projectId);
        validateProjectIsEnabled(project);

        if (Objects.isNull(project.getActiveProfileId())) {
            return null;
        }

        var profileOptional = profileRepository.findByIdAndProjectIdAndEnabledTrue(project.getActiveProfileId(), projectId);
        var profile = profileOptional.orElseThrow(() -> new HubException("NO_ACTIVE_PROFILE", "error.noActiveProfile", 409, projectId));
        return new ProfileRecord(profile.getId(), profile.getProjectId(), profile.getName(), profile.getDescription(), profile.isEnabled(), profile.getCreatedAt(), profile.getUpdatedAt(), profile.getVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectContextRecord getProjectContextByProjectIdAndPrompt(String projectId, String prompt) {
        var project = findProjectById(projectId);
        validateProjectIsEnabled(project);
        validateContextConnection(projectId);

        if (Objects.isNull(project.getActiveProfileId())) {
            throw new HubException("NO_ACTIVE_PROFILE", "error.noActiveProfile", 409, projectId);
        }

        var profileOptional = profileRepository.findByIdAndProjectIdAndEnabledTrue(project.getActiveProfileId(), projectId);
        var profile = profileOptional.orElseThrow(() -> new HubException("NO_ACTIVE_PROFILE", "error.noActiveProfile", 409, projectId));
        var projectSettings = projectSettingsRepository.findById(projectId).orElseGet(ProjectSettings::new);

        var permanentBudgetTokens = (int) Math.floor(projectSettings.getContextBudgetTokens() * projectSettings.getPermanentQuotaPercent() / 100.0);
        var workflowEntityList = workflowRepository.findByProfileIdAndEnabledTrueOrderByCreatedAtAscIdAsc(profile.getId());
        var skillEntityList = skillRepository.findByProfileIdAndEnabledTrueOrderByCreatedAtAscIdAsc(profile.getId());
        var memoryEntityList = memoryRepository.findByProfileIdAndEnabledTrueOrderByCreatedAtAscIdAsc(profile.getId());
        var alwaysSkillList = skillEntityList.stream().filter(skill -> skill.getApplicationMode() == ApplicationMode.ALWAYS).toList();
        var alwaysMemoryList = memoryEntityList.stream().filter(memory -> memory.getApplicationMode() == ApplicationMode.ALWAYS).toList();
        var contextualSkillList = selectContextualSkills(skillEntityList, prompt, projectSettings.isContextualSelectionEnabled());
        var contextualMemoryList = selectContextualMemories(memoryEntityList, prompt, projectSettings.isContextualSelectionEnabled());

        var workflowResult = addWorkflowsToBudget(workflowEntityList, permanentBudgetTokens);
        var remainingBudgetTokens = permanentBudgetTokens - workflowResult.estimatedTokens();
        var alwaysSkillResult = addSkillsToBudget(alwaysSkillList, remainingBudgetTokens);
        remainingBudgetTokens -= alwaysSkillResult.estimatedTokens();
        var alwaysMemoryResult = addMemoriesToBudget(alwaysMemoryList, remainingBudgetTokens);
        remainingBudgetTokens -= alwaysMemoryResult.estimatedTokens();
        var contextualSkillResult = addSkillsToBudget(contextualSkillList, remainingBudgetTokens);
        remainingBudgetTokens -= contextualSkillResult.estimatedTokens();
        var contextualMemoryResult = addMemoriesToBudget(contextualMemoryList, remainingBudgetTokens);

        var workflowRecordList = workflowResult.resourceList();
        var skillRecordList = new ArrayList<>(alwaysSkillResult.resourceList());
        skillRecordList.addAll(contextualSkillResult.resourceList());
        var memoryRecordList = new ArrayList<>(alwaysMemoryResult.resourceList());
        memoryRecordList.addAll(contextualMemoryResult.resourceList());

        var totalEstimatedTokens = workflowResult.estimatedTokens() + alwaysSkillResult.estimatedTokens() + alwaysMemoryResult.estimatedTokens() + contextualSkillResult.estimatedTokens() + contextualMemoryResult.estimatedTokens();
        var omittedCount = workflowResult.omittedCount() + alwaysSkillResult.omittedCount() + alwaysMemoryResult.omittedCount() + contextualSkillResult.omittedCount() + contextualMemoryResult.omittedCount();
        var truncated = workflowResult.truncated() || alwaysSkillResult.truncated() || alwaysMemoryResult.truncated() || contextualSkillResult.truncated() || contextualMemoryResult.truncated();

        if (truncated) {
            throw new HubException("TRUNCATED", "error.truncated", 409);
        }

        var hasContextResource = hasAnyContextResource(workflowEntityList, alwaysSkillList, alwaysMemoryList, contextualSkillList, contextualMemoryList);

        if (permanentBudgetTokens == 0 && hasContextResource) {
            throw new HubException("CONTEXT_LIMIT", "error.contextLimit", 422);
        }

        return new ProjectContextRecord(projectId, profile.getId(), workflowRecordList, List.copyOf(skillRecordList), List.copyOf(memoryRecordList), totalEstimatedTokens, truncated, omittedCount, permanentBudgetTokens);
    }

    @Override
    @Transactional(readOnly = true)
    public ContextResourceRecord getContextResourceByProjectIdAndTypeAndResourceId(String projectId, String resourceType, String resourceId) {
        var project = findProjectById(projectId);
        validateProjectIsEnabled(project);
        validateContextConnection(projectId);

        if (Objects.isNull(project.getActiveProfileId())) {
            throw new HubException("NO_ACTIVE_PROFILE", "error.noActiveProfile", 409, projectId);
        }

        var normalizedResourceType = resourceType.toUpperCase(Locale.ROOT);

        return switch (normalizedResourceType) {
            case "WORKFLOW" -> {
                var workflowOptional = workflowRepository.findByIdAndProfileIdAndEnabledTrue(resourceId, project.getActiveProfileId());
                yield workflowOptional.map(this::toWorkflowContextResource).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, resourceId));
            }
            case "SKILL" -> {
                var skillOptional = skillRepository.findByIdAndProfileIdAndEnabledTrue(resourceId, project.getActiveProfileId());
                yield skillOptional.map(this::toSkillContextResource).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, resourceId));
            }
            case "MEMORY" -> {
                var memoryOptional = memoryRepository.findByIdAndProfileIdAndEnabledTrue(resourceId, project.getActiveProfileId());
                yield memoryOptional.map(this::toMemoryContextResource).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, resourceId));
            }
            default -> throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "resourceType");
        };
    }

    private Project findProjectById(String projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, projectId));
    }

    private void validateProjectIsEnabled(Project project) {
        if (!project.isEnabled()) {
            throw new HubException("PROJECT_INACTIVE", "error.projectInactive", 409, project.getId());
        }
    }

    private void validateContextConnection(String projectId) {
        var projectSettings = projectSettingsRepository.findById(projectId).orElseGet(ProjectSettings::new);

        if (!projectSettings.isContextRetrievalEnabled()) {
            throw new HubException("CONNECTION_UNAVAILABLE", "error.connectionUnavailable", 503, projectId);
        }

        var connection = mcpConnectionRepository.findByProjectIdAndConnectionType(projectId, ConnectionType.CONTEXT).orElse(null);

        if (Objects.nonNull(connection) && !connection.isEnabled()) {
            throw new HubException("CONNECTION_UNAVAILABLE", "error.connectionUnavailable", 503, projectId);
        }
    }

    private List<Skill> selectContextualSkills(List<Skill> skillEntityList, String prompt, boolean enabled) {
        if (!enabled || ValidateUtils.isInvalidString(prompt)) {
            return List.of();
        }
        var promptWordSet = wordsIn(prompt);
        var contextualSkillList = skillEntityList.stream().filter(skill -> skill.getApplicationMode() == ApplicationMode.CONTEXTUAL).filter(skill -> relevanceScore(skill, promptWordSet) > 0).toList();
        var skillRelevanceComparator = Comparator.comparingInt((Skill skill) -> relevanceScore(skill, promptWordSet)).reversed().thenComparing(Skill::getCreatedAt).thenComparing(Skill::getId);
        return contextualSkillList.stream().sorted(skillRelevanceComparator).toList();
    }

    private List<Memory> selectContextualMemories(List<Memory> memoryEntityList, String prompt, boolean enabled) {
        if (!enabled || ValidateUtils.isInvalidString(prompt)) {
            return List.of();
        }

        var promptWordSet = wordsIn(prompt);
        var contextualMemoryList = memoryEntityList.stream().filter(memory -> memory.getApplicationMode() == ApplicationMode.CONTEXTUAL).filter(memory -> relevanceScore(memory, promptWordSet) > 0).toList();
        var memoryRelevanceComparator = Comparator.comparingInt((Memory memory) -> relevanceScore(memory, promptWordSet)).reversed().thenComparing(Memory::getCreatedAt).thenComparing(Memory::getId);
        return contextualMemoryList.stream().sorted(memoryRelevanceComparator).toList();
    }

    private int relevanceScore(Skill skill, Set<String> promptWordSet) {
        var skillContent = skill.getName() + " " + skill.getDescription() + " " + skill.getInstructions() + " " + skill.getTagsJson();
        return relevanceScore(skillContent, promptWordSet);
    }

    private int relevanceScore(Memory memory, Set<String> promptWordSet) {
        var memoryContent = memory.getTitle() + " " + memory.getContent() + " " + memory.getTagsJson();
        return relevanceScore(memoryContent, promptWordSet);
    }

    private ContextBudgetResultRecord<ContextResourceRecord> addWorkflowsToBudget(List<Workflow> workflowEntityList, int budgetTokens) {
        var resourceList = new ArrayList<ContextResourceRecord>();
        var usedTokens = 0;
        var omittedCount = 0;
        var truncated = false;

        for (var workflow : workflowEntityList) {
            var content = workflow.getInstructions();
            var estimatedTokens = estimateTokens(workflow.getName() + workflow.getDescription() + content);
            var remainingTokens = budgetTokens - usedTokens;

            if (estimatedTokens <= remainingTokens) {
                resourceList.add(toWorkflowContextResource(workflow));
                usedTokens += estimatedTokens;
                continue;
            }

            if (remainingTokens < 12) {
                omittedCount++;
                truncated = true;
                continue;
            }

            var shortenedContent = truncateText(content, remainingTokens);
            var resource = toWorkflowContextResource(workflow);
            resourceList.add(new ContextResourceRecord(resource.resourceType(), resource.id(), resource.name(), resource.title(), resource.description(), null, shortenedContent, List.of(), null, true, estimateTokens(shortenedContent), "get_context_resource"));
            usedTokens += estimateTokens(shortenedContent);
            truncated = true;
        }

        return new ContextBudgetResultRecord<>(List.copyOf(resourceList), usedTokens, omittedCount, truncated);
    }

    private ContextBudgetResultRecord<ContextResourceRecord> addSkillsToBudget(List<Skill> skillEntityList, int budgetTokens) {
        var resourceList = new ArrayList<ContextResourceRecord>();
        var usedTokens = 0;
        var omittedCount = 0;
        var truncated = false;

        for (var skill : skillEntityList) {
            var estimatedTokens = estimateTokens(skill.getName() + skill.getDescription() + skill.getInstructions());
            var remainingTokens = budgetTokens - usedTokens;

            if (estimatedTokens <= remainingTokens) {
                resourceList.add(toSkillContextResource(skill));
                usedTokens += estimatedTokens;
                continue;
            }

            if (remainingTokens < 12) {
                omittedCount++;
                truncated = true;
                continue;
            }

            var shortenedInstructions = truncateText(skill.getInstructions(), remainingTokens);
            var resource = toSkillContextResource(skill);
            resourceList.add(new ContextResourceRecord(resource.resourceType(), resource.id(), resource.name(), null, resource.description(), null, shortenedInstructions, resource.tags(), resource.applicationMode(), true, estimateTokens(shortenedInstructions), "get_context_resource"));
            usedTokens += estimateTokens(shortenedInstructions);
            truncated = true;
        }

        return new ContextBudgetResultRecord<>(List.copyOf(resourceList), usedTokens, omittedCount, truncated);
    }

    private ContextBudgetResultRecord<ContextResourceRecord> addMemoriesToBudget(List<Memory> memoryEntityList, int budgetTokens) {
        var resourceList = new ArrayList<ContextResourceRecord>();
        var usedTokens = 0;
        var omittedCount = 0;
        var truncated = false;

        for (var memory : memoryEntityList) {
            var estimatedTokens = estimateTokens(memory.getTitle() + memory.getContent());
            var remainingTokens = budgetTokens - usedTokens;

            if (estimatedTokens <= remainingTokens) {
                resourceList.add(toMemoryContextResource(memory));
                usedTokens += estimatedTokens;
                continue;
            }

            if (remainingTokens < 12) {
                omittedCount++;
                truncated = true;
                continue;
            }

            var shortenedContent = truncateText(memory.getContent(), remainingTokens);
            var resource = toMemoryContextResource(memory);
            resourceList.add(new ContextResourceRecord(resource.resourceType(), resource.id(), null, resource.title(), null, shortenedContent, null, resource.tags(), resource.applicationMode(), true, estimateTokens(shortenedContent), "get_context_resource"));
            usedTokens += estimateTokens(shortenedContent);
            truncated = true;
        }

        return new ContextBudgetResultRecord<>(List.copyOf(resourceList), usedTokens, omittedCount, truncated);
    }

    private ContextResourceRecord toWorkflowContextResource(Workflow workflow) {
        var estimatedTokens = estimateTokens(workflow.getName() + workflow.getDescription() + workflow.getInstructions());
        return new ContextResourceRecord("WORKFLOW", workflow.getId(), workflow.getName(), null, workflow.getDescription(), null, workflow.getInstructions(), List.of(), null, false, estimatedTokens, null);
    }

    private ContextResourceRecord toSkillContextResource(Skill skill) {
        var estimatedTokens = estimateTokens(skill.getName() + skill.getDescription() + skill.getInstructions());
        return new ContextResourceRecord("SKILL", skill.getId(), skill.getName(), null, skill.getDescription(), null, skill.getInstructions(), tagsJsonDataConverter.fromJson(skill.getTagsJson()), skill.getApplicationMode(), false, estimatedTokens, null);
    }

    private ContextResourceRecord toMemoryContextResource(Memory memory) {
        var estimatedTokens = estimateTokens(memory.getTitle() + memory.getContent());
        return new ContextResourceRecord("MEMORY", memory.getId(), null, memory.getTitle(), null, memory.getContent(), null, tagsJsonDataConverter.fromJson(memory.getTagsJson()), memory.getApplicationMode(), false, estimatedTokens, null);
    }

    private int relevanceScore(String content, Set<String> promptWordSet) {
        var score = 0;

        for (var word : wordsIn(content)) {
            if (promptWordSet.contains(word)) {
                score++;
            }
        }

        return score;
    }

    private Set<String> wordsIn(String text) {
        var normalizedText = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        var wordSet = new HashSet<String>();

        for (var word : normalizedText.split("[^a-z0-9]+")) {
            if (word.length() >= 3) {
                wordSet.add(word);
            }
        }

        return wordSet;
    }

    private int estimateTokens(String text) {
        var characterCount = text.codePointCount(0, text.length());
        return (int) Math.ceil(Math.ceil(characterCount / 3.0) * 1.1);
    }

    private String truncateText(String text, int tokenBudget) {
        var maximumCharacters = Math.max(1, (int) Math.floor(tokenBudget * 30.0 / 11.0));
        var characterCount = text.codePointCount(0, text.length());

        if (characterCount <= maximumCharacters) {
            return text;
        }

        var finalIndex = text.offsetByCodePoints(0, maximumCharacters - 1);
        return text.substring(0, finalIndex) + "…";
    }

    private boolean hasAnyContextResource(List<?>... contextResourceList) {
        for (var resourceList : contextResourceList) {
            if (!resourceList.isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
