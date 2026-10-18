package io.agentmemoryhub.core.service.task;

import io.agentmemoryhub.core.common.TaskCreationMode;
import io.agentmemoryhub.core.common.ConnectionType;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.Project;
import io.agentmemoryhub.core.model.domain.ProjectSettings;
import io.agentmemoryhub.core.model.domain.Task;
import io.agentmemoryhub.core.model.domain.TaskBlock;
import io.agentmemoryhub.core.model.record.TaskAnalysisRecord;
import io.agentmemoryhub.core.model.record.TaskContextBlockRecord;
import io.agentmemoryhub.core.model.record.TaskContextRecord;
import io.agentmemoryhub.core.model.record.TaskContextTaskRecord;
import io.agentmemoryhub.core.model.record.TaskContinuityRecord;
import io.agentmemoryhub.core.model.record.TaskInputRecord;
import io.agentmemoryhub.core.model.record.TaskPageRecord;
import io.agentmemoryhub.core.model.record.TaskRecord;
import io.agentmemoryhub.core.model.record.WorkspaceReferenceRecord;
import io.agentmemoryhub.core.repository.ProfileRepository;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.ProjectSettingsRepository;
import io.agentmemoryhub.core.repository.McpConnectionRepository;
import io.agentmemoryhub.core.repository.TaskBlockRepository;
import io.agentmemoryhub.core.repository.TaskRepository;
import io.agentmemoryhub.core.service.idempotency.IdempotencyKeyService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final TaskBlockRepository taskBlockRepository;
    private final ProfileRepository profileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectSettingsRepository projectSettingsRepository;
    private final McpConnectionRepository mcpConnectionRepository;
    private final IdempotencyKeyService idempotencyKeyService;

    @Value("${agent-memory-hub.server-url:https://localhost:8443}")
    private String serverUrl;

    @Override
    @Transactional(readOnly = true)
    public TaskPageRecord listTasksByProfileId(String profileId, int page, int pageSize) {
        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var taskEntityList = taskRepository.findByProfileIdOrderByCreatedAtDescIdAsc(profileId);
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, taskEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, taskEntityList.size());
        var taskRecordList = taskEntityList.subList(firstIndex, lastIndex).stream().map(this::toTaskRecord).toList();

        return new TaskPageRecord(taskRecordList, normalizedPage, normalizedPageSize, taskEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskRecord getTaskById(String taskId) {
        return toTaskRecord(findTaskById(taskId));
    }

    @Override
    @Transactional
    public TaskRecord createTaskByProfileIdAndInput(String profileId, TaskInputRecord taskInputRecord, String requestId) {
        validateTaskInput(taskInputRecord, true);
        var profile = profileRepository.findByIdAndEnabledTrue(profileId).orElseThrow(() -> new HubException("PROFILE_INACTIVE", "error.profileInactive", 409, profileId));
        var project = projectRepository.findById(profile.getProjectId()).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, profile.getProjectId()));

        if (!project.isEnabled()) {
            throw new HubException("PROJECT_INACTIVE", "error.projectInactive", 409, project.getId());
        }

        if (!profileId.equals(project.getActiveProfileId())) {
            throw new HubException("NO_ACTIVE_PROFILE", "error.noActiveProfile", 409, project.getId());
        }

        var task = new Task();
        task.setProfileId(profileId);
        task.setTitle(taskInputRecord.title().strip());
        task.setDescription(taskInputRecord.description().strip());
        task.setSummary(Objects.requireNonNullElse(taskInputRecord.summary(), ""));

        if (ValidateUtils.isValidString(requestId)) {
            var projectId = project.getId();
            var requestContent = taskInputRecord.toString();
            var newTaskId = task.getId();
            var reservedTaskId = idempotencyKeyService.reserveResourceIdByRequest(projectId, "create_task", requestId, requestContent, newTaskId);

            if (!reservedTaskId.equals(task.getId())) {
                var existingTaskOptional = taskRepository.findById(reservedTaskId);
                var existingTask = existingTaskOptional.orElseThrow(() -> new HubException("CONFLICT", "error.conflict", 409, reservedTaskId));
                return toTaskRecord(existingTask);
            }
        }

        return toTaskRecord(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskRecord updateTaskById(String taskId, TaskInputRecord taskInputRecord, Long expectedVersion) {
        validateTaskInput(taskInputRecord, false);
        var task = findTaskById(taskId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion != task.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, taskId);
        }

        if (Objects.nonNull(taskInputRecord.title())) {
            if (ValidateUtils.isInvalidString(taskInputRecord.title())) {
                throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "title");
            }

            task.setTitle(taskInputRecord.title().strip());
        }
        if (Objects.nonNull(taskInputRecord.description())) {
            if (ValidateUtils.isInvalidString(taskInputRecord.description())) {
                throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "description");
            }

            task.setDescription(taskInputRecord.description().strip());
        }

        if (Objects.nonNull(taskInputRecord.summary())) {
            task.setSummary(taskInputRecord.summary());
        }

        return toTaskRecord(taskRepository.save(task));
    }

    @Override
    @Transactional
    public void deleteTaskById(String taskId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        taskRepository.delete(findTaskById(taskId));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskContextRecord getTaskContextByProjectIdAndTaskId(String projectId, String taskId, String prompt) {
        var project = findProjectById(projectId);
        validateProjectForRecovery(project);
        var activeProfileOptional = profileRepository.findByIdAndProjectIdAndEnabledTrue(project.getActiveProfileId(), projectId);
        var activeProfile = activeProfileOptional.orElseThrow(() -> new HubException("NO_ACTIVE_PROFILE", "error.noActiveProfile", 409, projectId));
        var task = taskRepository.findByIdAndProfileId(taskId, activeProfile.getId()).orElseThrow(() -> new HubException("PROFILE_MISMATCH", "error.invalidRelation", 409, taskId));
        var projectSettings = projectSettingsRepository.findById(projectId).orElseGet(ProjectSettings::new);
        var taskBudgetTokens = (int) Math.floor(projectSettings.getContextBudgetTokens() * (100 - projectSettings.getPermanentQuotaPercent()) / 100.0);
        var taskBlockEntityList = taskBlockRepository.findByTaskIdOrderByCreatedAtAscIdAsc(taskId);
        var selectedTaskBlockList = selectRelevantTaskBlocks(taskBlockEntityList, prompt);
        var taskContextTaskRecord = toTaskContextTaskRecord(task, taskBudgetTokens);
        var usedBudgetTokens = taskContextTaskRecord.estimatedTokens();
        var omittedCount = Math.max(0, taskBlockEntityList.size() - selectedTaskBlockList.size());
        var taskContextBlockRecordList = new ArrayList<TaskContextBlockRecord>();

        for (var taskBlock : selectedTaskBlockList) {
            var blockEstimatedTokens = estimateTokens(taskBlock.getContent());
            var remainingBudgetTokens = taskBudgetTokens - usedBudgetTokens;

            if (blockEstimatedTokens <= remainingBudgetTokens) {
                var taskContextBlockRecord = new TaskContextBlockRecord(taskBlock.getId(), taskBlock.getTaskId(), taskBlock.getContent(), taskBlock.getCreatedAt(), taskBlock.getUpdatedAt(), taskBlock.getVersion(), false, blockEstimatedTokens, null);
                taskContextBlockRecordList.add(taskContextBlockRecord);
                usedBudgetTokens += blockEstimatedTokens;
                continue;
            }

            var abbreviatedContent = truncateText(taskBlock.getContent(), "", remainingBudgetTokens);

            if (abbreviatedContent.isEmpty()) {
                omittedCount++;
                continue;
            }

            var abbreviatedEstimatedTokens = estimateTokens(abbreviatedContent);
            var taskContextBlockRecord = new TaskContextBlockRecord(taskBlock.getId(), taskBlock.getTaskId(), abbreviatedContent, taskBlock.getCreatedAt(), taskBlock.getUpdatedAt(), taskBlock.getVersion(), true, abbreviatedEstimatedTokens, "get_task_block");
            taskContextBlockRecordList.add(taskContextBlockRecord);
            usedBudgetTokens += abbreviatedEstimatedTokens;
        }

        var truncated = taskContextTaskRecord.truncated() || taskContextBlockRecordList.stream().anyMatch(TaskContextBlockRecord::truncated) || omittedCount > 0;
        return new TaskContextRecord(taskContextTaskRecord, List.copyOf(taskContextBlockRecordList), true, usedBudgetTokens, truncated, omittedCount, taskBudgetTokens);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceReferenceRecord getWorkspaceReferenceGuidanceByProjectIdAndTaskId(String projectId, String taskId) {
        var project = findProjectById(projectId);

        if (ValidateUtils.isValidString(taskId)) {
            var task = findTaskById(taskId);
            var profile = profileRepository.findById(task.getProfileId()).orElseThrow(() -> new HubException("INVALID_RELATION", "error.invalidRelation", 409, taskId));
            if (!projectId.equals(profile.getProjectId())) {
                throw new HubException("INVALID_RELATION", "error.invalidRelation", 409, taskId);
            }
        }

        var instructionsList = List.of(
                "workspace.guidance.read",
                "workspace.guidance.activeProfile",
                "workspace.guidance.writeTask",
                "workspace.guidance.gitExclude"
        );

        return new WorkspaceReferenceRecord(".agent-memory-hub/workspace.json", 1, serverUrl, projectId, taskId, instructionsList);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAnalysisRecord analyzeTaskPromptByProjectIdAndTaskId(String projectId, String prompt, String taskId) {
        var project = findProjectById(projectId);
        validateProjectForTaskAnalysis(project);
        var projectSettings = projectSettingsRepository.findById(projectId).orElseGet(ProjectSettings::new);
        var mode = projectSettings.getTaskCreationMode();
        var complexity = mode == TaskCreationMode.MANUAL ? "SIMPLE" : classifyComplexity(prompt);
        var hasSelectedTask = hasValidSelectedTask(project, taskId);
        var shouldSuggest = !hasSelectedTask && mode == TaskCreationMode.SUGGEST && !"SIMPLE".equals(complexity);
        var shouldCreateAutomatically = !hasSelectedTask && mode == TaskCreationMode.AUTOMATIC && !"SIMPLE".equals(complexity);
        var reason = hasSelectedTask ? "task.analysis.reason.selected" : mode == TaskCreationMode.MANUAL ? "task.analysis.reason.manual" : "task.analysis.reason.classified";
        var suggestedTitle = shouldSuggest || shouldCreateAutomatically ? suggestedTitle(prompt) : null;
        var suggestedDescription = shouldSuggest || shouldCreateAutomatically ? prompt.strip() : null;
        return new TaskAnalysisRecord(mode, complexity, shouldSuggest, shouldCreateAutomatically, reason, suggestedTitle, suggestedDescription);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskContinuityRecord inspectTaskContinuityByProjectIdAndTaskId(String projectId, String taskId) {
        var project = findProjectById(projectId);
        var activeProfileId = project.getActiveProfileId();

        if (!project.isEnabled()) {
            return new TaskContinuityRecord(projectId, activeProfileId, null, false, "PROJECT_INACTIVE", 0);
        }

        if (Objects.isNull(activeProfileId)) {
            return new TaskContinuityRecord(projectId, null, null, false, "NO_ACTIVE_PROFILE", 0);
        }

        if (ValidateUtils.isInvalidString(taskId)) {
            return new TaskContinuityRecord(projectId, activeProfileId, null, false, "TASK_NOT_SELECTED", 0);
        }

        var task = taskRepository.findById(taskId).orElse(null);

        if (Objects.isNull(task)) {
            return new TaskContinuityRecord(projectId, activeProfileId, null, false, "TASK_NOT_FOUND", 0);
        }

        var valid = activeProfileId.equals(task.getProfileId());
        var blockCount = Math.toIntExact(taskBlockRepository.countByTaskId(taskId));
        return new TaskContinuityRecord(projectId, activeProfileId, toTaskRecord(task), valid, valid ? null : "PROFILE_MISMATCH", blockCount);
    }

    private Task findTaskById(String taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, taskId));
    }

    private Project findProjectById(String projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, projectId));
    }

    private void validateTaskInput(TaskInputRecord taskInputRecord, boolean create) {
        if (Objects.isNull(taskInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "task");
        }

        if (!create && Objects.isNull(taskInputRecord.title()) && Objects.isNull(taskInputRecord.description()) && Objects.isNull(taskInputRecord.summary())) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "changes");
        }

        if (create && (ValidateUtils.isInvalidString(taskInputRecord.title()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "title");
        }

        if (create && (ValidateUtils.isInvalidString(taskInputRecord.description()))) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "description");
        }
    }

    private void validateProjectForRecovery(Project project) {
        if (!project.isEnabled()) {
            throw new HubException("PROJECT_INACTIVE", "error.projectInactive", 409, project.getId());
        }

        if (Objects.isNull(project.getActiveProfileId())) {
            throw new HubException("NO_ACTIVE_PROFILE", "error.noActiveProfile", 409, project.getId());
        }

        var projectSettings = projectSettingsRepository.findById(project.getId()).orElseGet(ProjectSettings::new);
        var taskConnection = mcpConnectionRepository.findByProjectIdAndConnectionType(project.getId(), ConnectionType.TASKS).orElse(null);
        var taskConnectionIsUnavailable = Objects.nonNull(taskConnection) && !taskConnection.isEnabled();

        if (!projectSettings.isSelectedTaskRetrievalEnabled() || taskConnectionIsUnavailable) {
            throw new HubException("CONNECTION_UNAVAILABLE", "error.connectionUnavailable", 503, project.getId());
        }
    }

    private void validateProjectForTaskAnalysis(Project project) {
        if (!project.isEnabled()) {
            throw new HubException("PROJECT_INACTIVE", "error.projectInactive", 409, project.getId());
        }

        var activeProfileId = project.getActiveProfileId();
        var activeProfileIsUnavailable = Objects.isNull(activeProfileId) || profileRepository.findByIdAndProjectIdAndEnabledTrue(activeProfileId, project.getId()).isEmpty();

        if (activeProfileIsUnavailable) {
            throw new HubException("NO_ACTIVE_PROFILE", "error.noActiveProfile", 409, project.getId());
        }
    }

    private boolean hasValidSelectedTask(Project project, String taskId) {
        if (ValidateUtils.isInvalidString(taskId) || Objects.isNull(project.getActiveProfileId())) {
            return false;
        }

        return taskRepository.findByIdAndProfileId(taskId, project.getActiveProfileId()).isPresent();
    }

    private String classifyComplexity(String prompt) {
        if (ValidateUtils.isInvalidString(prompt)) {
            return "SIMPLE";
        }

        var normalizedPrompt = normalizeText(prompt);
        var queryOnly = normalizedPrompt.startsWith("what ") || normalizedPrompt.startsWith("how ") || normalizedPrompt.startsWith("why ") || normalizedPrompt.startsWith("que ") || normalizedPrompt.startsWith("como ") || normalizedPrompt.startsWith("por que ") || normalizedPrompt.endsWith("?");

        if (queryOnly) {
            return "SIMPLE";
        }

        var conjunctionCount = normalizedPrompt.split("\\b(and|also|plus|ademas|tambien)\\b").length - 1;
        var promptLength = prompt.codePointCount(0, prompt.length());
        var promptIsVeryLong = promptLength > 900;
        var longPromptHasSeveralConjunctions = promptLength > 400 && conjunctionCount >= 2;

        if (promptIsVeryLong || longPromptHasSeveralConjunctions) {
            return "COMPLEX";
        }

        var promptIsLong = promptLength > 240;
        var promptHasConjunctions = conjunctionCount > 0;

        if (promptIsLong || promptHasConjunctions) {
            return "MODERATE";
        }

        return "SIMPLE";
    }

    private String suggestedTitle(String prompt) {
        var firstLine = prompt.strip().lines().findFirst().orElse("New task").strip();

        if (firstLine.codePointCount(0, firstLine.length()) <= 120) {
            return firstLine;
        }

        var finalIndex = firstLine.offsetByCodePoints(0, 117);
        return firstLine.substring(0, finalIndex) + "...";
    }

    private List<TaskBlock> selectRelevantTaskBlocks(List<TaskBlock> taskBlockEntityList, String prompt) {
        if (ValidateUtils.isInvalidString(prompt)) {
            return taskBlockEntityList;
        }

        var promptWordSet = wordsIn(prompt);

        if (promptWordSet.isEmpty()) {
            return taskBlockEntityList;
        }

        var relevantTaskBlockList = taskBlockEntityList.stream().filter(taskBlock -> wordsIn(taskBlock.getContent()).stream().anyMatch(promptWordSet::contains)).toList();
        var taskBlockRelevanceComparator = Comparator.comparingInt((TaskBlock taskBlock) -> relevanceScore(taskBlock.getContent(), promptWordSet)).reversed().thenComparing(TaskBlock::getCreatedAt).thenComparing(TaskBlock::getId);
        return relevantTaskBlockList.stream().sorted(taskBlockRelevanceComparator).toList();
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
        var normalizedText = normalizeText(text);
        var wordSet = new HashSet<String>();

        for (var word : normalizedText.split("[^a-z0-9]+")) {
            if (word.length() >= 3) {
                wordSet.add(word);
            }
        }

        return wordSet;
    }

    private String normalizeText(String text) {
        var decomposedText = Normalizer.normalize(text, Normalizer.Form.NFD);
        var accentFreeText = decomposedText.replaceAll("\\p{M}+", "");
        var lowercaseText = accentFreeText.toLowerCase(Locale.ROOT);
        return lowercaseText.replaceAll("\\s+", " ").strip();
    }

    private int estimateTokens(String text) {
        var characterCount = text.codePointCount(0, text.length());
        return (int) Math.ceil(Math.ceil(characterCount / 3.0) * 1.1);
    }

    private String truncateText(String text, String prefix, int tokenBudget) {
        var characterCount = text.codePointCount(0, text.length());

        if (estimateTokens(prefix + text) <= tokenBudget) {
            return text;
        }

        var minimumCharacterCount = 0;
        var maximumCharacterCount = characterCount;

        while (minimumCharacterCount < maximumCharacterCount) {
            var candidateCharacterCount = (minimumCharacterCount + maximumCharacterCount + 1) / 2;
            var candidateEndIndex = text.offsetByCodePoints(0, candidateCharacterCount);
            var candidateText = text.substring(0, candidateEndIndex) + "…";

            if (estimateTokens(prefix + candidateText) <= tokenBudget) {
                minimumCharacterCount = candidateCharacterCount;
            } else {
                maximumCharacterCount = candidateCharacterCount - 1;
            }
        }

        if (minimumCharacterCount == 0) {
            return estimateTokens(prefix + "…") <= tokenBudget ? "…" : "";
        }

        var finalIndex = text.offsetByCodePoints(0, minimumCharacterCount);
        return text.substring(0, finalIndex) + "…";
    }

    private TaskContextTaskRecord toTaskContextTaskRecord(Task task, int tokenBudget) {
        var totalTokens = estimateTokens(task.getTitle() + task.getDescription() + task.getSummary());

        if (totalTokens <= tokenBudget) {
            return new TaskContextTaskRecord(task.getId(), task.getProfileId(), task.getTitle(), task.getDescription(), task.getSummary(), task.getCreatedAt(), task.getUpdatedAt(), task.getVersion(), false, totalTokens, null);
        }

        var abbreviatedTitle = truncateText(task.getTitle(), "", tokenBudget);
        var titleTokens = estimateTokens(abbreviatedTitle);
        var descriptionBudget = titleTokens + Math.max(0, (tokenBudget - titleTokens) / 2);
        var abbreviatedDescription = truncateText(task.getDescription(), abbreviatedTitle, descriptionBudget);
        var abbreviatedSummary = truncateText(task.getSummary(), abbreviatedTitle + abbreviatedDescription, tokenBudget);
        var estimatedTokens = estimateTokens(abbreviatedTitle + abbreviatedDescription + abbreviatedSummary);
        return new TaskContextTaskRecord(task.getId(), task.getProfileId(), abbreviatedTitle, abbreviatedDescription, abbreviatedSummary, task.getCreatedAt(), task.getUpdatedAt(), task.getVersion(), true, estimatedTokens, "get_task");
    }

    private TaskRecord toTaskRecord(Task task) {
        return new TaskRecord(task.getId(), task.getProfileId(), task.getTitle(), task.getDescription(), task.getSummary(), task.getCreatedAt(), task.getUpdatedAt(), task.getVersion());
    }
}
