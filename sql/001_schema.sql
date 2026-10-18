-- Agent Memory Hub schema, version 1. Execute manually on a new database.
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(512) NOT NULL,
    platform_role VARCHAR(16) NOT NULL CHECK (platform_role IN ('ADMIN', 'USER')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE projects (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    owner_user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    active_profile_id VARCHAR(36),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE project_members (
    project_id VARCHAR(36) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    project_role VARCHAR(16) NOT NULL CHECK (project_role IN ('OWNER', 'EDITOR', 'READER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE profiles (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1,
    UNIQUE (id, project_id)
);

ALTER TABLE projects ADD CONSTRAINT fk_project_active_profile
    FOREIGN KEY (active_profile_id, id) REFERENCES profiles(id, project_id) ON DELETE RESTRICT;

CREATE TABLE skills (
    id VARCHAR(36) PRIMARY KEY,
    profile_id VARCHAR(36) NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    instructions TEXT NOT NULL,
    tags_json TEXT NOT NULL DEFAULT '[]',
    application_mode VARCHAR(16) NOT NULL CHECK (application_mode IN ('ALWAYS', 'CONTEXTUAL')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE workflows (
    id VARCHAR(36) PRIMARY KEY,
    profile_id VARCHAR(36) NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    instructions TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE memories (
    id VARCHAR(36) PRIMARY KEY,
    profile_id VARCHAR(36) NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    content TEXT NOT NULL,
    tags_json TEXT NOT NULL DEFAULT '[]',
    application_mode VARCHAR(16) NOT NULL CHECK (application_mode IN ('ALWAYS', 'CONTEXTUAL')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY,
    profile_id VARCHAR(36) NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    summary TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE task_blocks (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE memory_proposals (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    proposed_content TEXT NOT NULL,
    reason TEXT NOT NULL,
    tags_json TEXT NOT NULL DEFAULT '[]',
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    result_memory_id VARCHAR(36) REFERENCES memories(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE mcp_connections (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    connection_type VARCHAR(16) NOT NULL CHECK (connection_type IN ('CONTEXT', 'TASKS')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1,
    UNIQUE (project_id, connection_type)
);

CREATE TABLE project_settings (
    project_id VARCHAR(36) PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE,
    context_retrieval_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    compact_recovery_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    contextual_selection_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    task_creation_mode VARCHAR(16) NOT NULL DEFAULT 'MANUAL' CHECK (task_creation_mode IN ('MANUAL', 'SUGGEST', 'AUTOMATIC')),
    automatic_summary_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    selected_task_retrieval_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    context_budget_tokens INTEGER NOT NULL DEFAULT 6000 CHECK (context_budget_tokens BETWEEN 100 AND 100000),
    permanent_quota_percent INTEGER NOT NULL DEFAULT 70 CHECK (permanent_quota_percent BETWEEN 0 AND 100),
    pre_tool_use_context_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1,
    CHECK (permanent_quota_percent + (100 - permanent_quota_percent) = 100)
);

CREATE TABLE idempotency_keys (
    project_id VARCHAR(36) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    operation VARCHAR(100) NOT NULL,
    request_id VARCHAR(200) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    resource_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, operation, request_id)
);

CREATE TABLE auth_sessions (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_profiles_project ON profiles(project_id, created_at, id);
CREATE INDEX ix_skills_profile ON skills(profile_id, created_at, id);
CREATE INDEX ix_workflows_profile ON workflows(profile_id, enabled, created_at, id);
CREATE INDEX ix_memories_profile ON memories(profile_id, created_at, id);
CREATE INDEX ix_tasks_profile ON tasks(profile_id, created_at DESC, id);
CREATE INDEX ix_task_blocks_task ON task_blocks(task_id, created_at, id);
CREATE INDEX ix_proposals_task ON memory_proposals(task_id, processed, created_at, id);
CREATE INDEX ix_members_user ON project_members(user_id, project_id);
CREATE INDEX ix_auth_sessions_expiry ON auth_sessions(expires_at);
