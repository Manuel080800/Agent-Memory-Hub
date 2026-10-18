---
name: setup
description: Link a Codex workspace to a local Agent Memory Hub project for independent task continuity.
---

# Set up Agent Memory Hub Tasks

1. Confirm that Agent Memory Hub is running locally and that `agent-memory-hub-tasks` is available. If not, point to the repository setup guide; do not start containers or change system trust settings without a direct request.
2. Ask the user which existing Hub project to link. Never infer a project from the repository name.
3. Call `inspect_task_continuity` with the supplied `projectId` and no `taskId` to validate the project and obtain the active profile. Do not choose a profile automatically.
4. Ask whether the user wants to select an existing task or leave `taskId` null. Validate a supplied task with `inspect_task_continuity` and require `valid: true` before using it.
5. Call `get_workspace_reference_guidance` with the validated project and selected task identifier. Only after user confirmation, write `.agent-memory-hub/workspace.json` with Codex native file tools using the returned values. Do not add secrets or Hub content.
6. If the project or task cannot be validated, explain why and leave the local reference unchanged.

The context server is a separate plugin. Install and configure Agent Memory Hub Context independently when permanent project context is needed.
