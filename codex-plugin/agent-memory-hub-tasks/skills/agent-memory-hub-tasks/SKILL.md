---
name: agent-memory-hub-tasks
description: Use the Agent Memory Hub Tasks server for explicit task continuity, task analysis, and task block operations.
---

# Agent Memory Hub Tasks workflow

## Validate local task continuity

- Read `.agent-memory-hub/workspace.json` with native workspace file tools when the request belongs to a linked worktree.
- Treat `projectId` and `taskId` as opaque values. Never derive identifiers from directory names.
- Use `inspect_task_continuity` to validate the project, active profile, and selected task before relying on task context.
- If the project is missing, inactive, or lacks an active profile, state that task context is unavailable. Never invent a replacement.
- Use `get_task_context` only for a validated selected task. Use task/block full-read tools when returned content is truncated.

## Task creation and updates

- Task creation is manual by default. Create a task only when the user explicitly asks, or after the user accepts a suggestion allowed by the project's configured mode.
- `analyze_task_prompt` is read-only and never creates a task. In `SUGGEST` mode, ask at most once for the current new work request and create only after explicit acceptance. In `AUTOMATIC` mode, create only for a moderate or complex new work request and only by calling `create_task`.
- Obtain the active profile from `inspect_task_continuity`. After `create_task` succeeds, use the returned `taskId` and `get_workspace_reference_guidance`, then use native file tools to write the local reference. The MCP server never writes local files. If file writing fails, report that the task exists but is not selected locally; do not create a duplicate.
- Update task summaries only on explicit request, unless the project setting enables automatic summary updates and a meaningful decision, objective change, major progress, or important pending item makes an update useful. Do not update once per prompt.
- Use task blocks for separate durable details. Never store conversation transcripts as blocks.

## Destructive actions

- Before destructive MCP operations, summarize the expected effects and get the explicit confirmation required by the tool.
- The local MCP server is anonymous and intended for a trusted local machine. Do not claim that it enforces a human user's project role.
- Permanent context and memory operations belong to the separate Agent Memory Hub Context plugin.
