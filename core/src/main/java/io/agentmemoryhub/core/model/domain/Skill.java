package io.agentmemoryhub.core.model.domain;

import io.agentmemoryhub.core.common.ApplicationMode;
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
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "skills")
public class Skill {
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

    @Column(name = "profile_id", length = 36, nullable = false)
    private String profileId;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String instructions;

    @Column(name = "tags_json", nullable = false)
    private String tagsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_mode", length = 16, nullable = false)
    private ApplicationMode applicationMode = ApplicationMode.CONTEXTUAL;

    private boolean enabled = true;

    @PrePersist
    protected void setCreationTimestamps() {
        var now = Instant.now();
        if (Objects.isNull(createdAt)) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void setUpdateTimestamp() {
        updatedAt = Instant.now();
    }
}
