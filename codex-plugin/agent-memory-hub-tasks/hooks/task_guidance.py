#!/usr/bin/env python3

import json
import sys


def main():
    event = json.load(sys.stdin).get("hook_event_name", "")
    if event == "SessionStart":
        additional_context = (
            "For work linked to Agent Memory Hub, use the Tasks MCP server only for temporary "
            "continuity. Read the project's taskId from .agent-memory-hub/workspace.json with "
            "native file tools, check get_project_settings, and validate it with inspect_task_continuity. "
            "Only retrieve task context automatically when selected-task retrieval and compact recovery "
            "are enabled. Never infer project or task identifiers, and do not create a task from a hook."
        )
        print(json.dumps({
            "hookSpecificOutput": {
                "hookEventName": event,
                "additionalContext": additional_context
            }
        }))
        return

    if event == "UserPromptSubmit":
        additional_context = (
            "For a linked Agent Memory Hub project, use analyze_task_prompt only when task "
            "creation is relevant. Analysis is read-only. Follow the project's MANUAL, SUGGEST, "
            "or explicitly enabled AUTOMATIC mode; check get_project_settings before automatic summary "
            "updates. Never create a task merely because this hook ran."
        )
        print(json.dumps({
            "hookSpecificOutput": {
                "hookEventName": event,
                "additionalContext": additional_context
            }
        }))
        return

    if event == "PreCompact":
        system_message = (
            "Before compaction, preserve the linked Hub project/task identifiers, decisions, and "
            "next steps in the conversation summary. This hook does not write to Agent Memory Hub."
        )
    elif event == "PostCompact":
        system_message = (
            "After compaction, reread the local workspace reference and project settings, then validate "
            "any selected task through the Tasks MCP server. Retrieve task context only when compact "
            "recovery and selected-task retrieval are enabled. State when the Hub is unavailable."
        )
    elif event == "Stop":
        system_message = (
            "If work continues later, preserve its current project/task identifiers and next step. "
            "Do not block, create a task, continue the turn, or write a Hub summary from this hook."
        )
    else:
        system_message = "Agent Memory Hub Tasks hooks provide guidance only; use the Tasks MCP server for task data."

    print(json.dumps({"systemMessage": system_message}))


if __name__ == "__main__":
    main()
