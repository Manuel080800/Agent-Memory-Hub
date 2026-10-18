---
name: setup
description: Connect a Codex workspace to the independent Agent Memory Hub Context server.
---

# Set up Agent Memory Hub Context

1. Confirm that Agent Memory Hub is running locally and that the `agent-memory-hub-context` server is available. If it is unavailable, point to the repository setup guide; do not start containers or change system trust settings without a direct request.
2. Call `list_projects`, show the valid Hub project identifiers, and ask the user which project to link. Never infer a project from a repository name.
3. Call `get_project` to validate the selected identifier and `get_active_profile` to discover the active profile. Do not select a profile automatically.
4. After the user confirms the workspace link, create or update `.agent-memory-hub/workspace.json` using Codex's native file tools. Store `version: 1`, the local Hub `serverUrl`, and the validated `projectId`. Preserve any existing `taskId`; if there is no task selection, store `taskId: null`. Do not add secrets or Hub content.
5. The task server is a separate plugin. Install and configure Agent Memory Hub Tasks independently when task continuity is needed.

If the project cannot be linked, explain why and leave the local reference unchanged. This plugin provides permanent context only; its hooks are guidance, while tool results are the current Hub data.
