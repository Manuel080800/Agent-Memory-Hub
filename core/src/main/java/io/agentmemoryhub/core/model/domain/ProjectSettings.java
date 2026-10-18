package io.agentmemoryhub.core.model.domain;

import io.agentmemoryhub.core.common.TaskCreationMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "project_settings")
public class ProjectSettings {
    @Id
    @Column(name = "project_id", length = 36, nullable = false, updatable = false)
    private String projectId;

    @Column(name = "context_retrieval_enabled", nullable = false)
    private boolean contextRetrievalEnabled = true;

    @Column(name = "compact_recovery_enabled", nullable = false)
    private boolean compactRecoveryEnabled = true;

    @Column(name = "contextual_selection_enabled", nullable = false)
    private boolean contextualSelectionEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_creation_mode", length = 16, nullable = false)
    private TaskCreationMode taskCreationMode = TaskCreationMode.MANUAL;

    @Column(name = "automatic_summary_enabled", nullable = false)
    private boolean automaticSummaryEnabled;

    @Column(name = "selected_task_retrieval_enabled", nullable = false)
    private boolean selectedTaskRetrievalEnabled = true;

    @Column(name = "context_budget_tokens", nullable = false)
    private int contextBudgetTokens = 6000;

    @Column(name = "permanent_quota_percent", nullable = false)
    private int permanentQuotaPercent = 70;

    @Column(name = "pre_tool_use_context_enabled", nullable = false)
    private boolean preToolUseContextEnabled;

    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Setter(AccessLevel.NONE)
    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    protected void setCreationTimestamp() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void setUpdateTimestamp() {
        updatedAt = Instant.now();
    }
}
