#!/usr/bin/env python3

import json
import sys


def main():
    event = json.load(sys.stdin).get("hook_event_name", "")
    if event == "SessionStart":
        additional_context = (
            "For work associated with Agent Memory Hub, use Codex's native file tools to read "
            ".agent-memory-hub/workspace.json. Use its projectId to check get_project_settings "
            "before automatic context retrieval. Retrieve current permanent context through the "
            "Agent Memory Hub Context server only when project settings allow it. The active profile "
            "always comes from the Hub. Do not invent a project or select a profile automatically."
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
            "When this request concerns a project linked to Agent Memory Hub, consult its local "
            "workspace reference with native file tools, check project settings, then use the Context "
            "MCP server's read tools for current permanent context when retrieval is enabled. Do not "
            "create or change permanent memory from this hook."
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
            "Before compaction, preserve the active Hub project/task identifiers and important "
            "decisions in the conversation summary. This hook does not write to Agent Memory Hub."
        )
    elif event == "PostCompact":
        system_message = (
            "After compaction, reread the local workspace reference and project settings. Retrieve "
            "current permanent Hub context only when compact recovery and context retrieval are enabled. "
            "State when the Hub is unavailable."
        )
    elif event == "Stop":
        system_message = (
            "If this work continues later, preserve its current project/task identifiers and next step "
            "in the response. Do not block, continue the turn, or write a Hub summary from this hook."
        )
    else:
        system_message = "Agent Memory Hub hooks provide guidance only; use MCP for Hub data."

    print(json.dumps({"systemMessage": system_message}))


if __name__ == "__main__":
    main()
