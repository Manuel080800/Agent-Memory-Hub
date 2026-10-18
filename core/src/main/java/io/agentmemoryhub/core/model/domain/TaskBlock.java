package io.agentmemoryhub.core.model.domain;

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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task_blocks")
public class TaskBlock {
    @Setter(AccessLevel.NONE)
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Setter(AccessLevel.NONE)
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;

    @Column(nullable = false)
    private String content;

    @PrePersist
    protected void setCreationTimestamps() {
        var now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void setUpdateTimestamp() {
        updatedAt = Instant.now();
    }
}
