#!/usr/bin/env python3

import argparse
import json
import zipfile
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
MARKETPLACE_ROOT = REPOSITORY_ROOT / "codex-plugin"
PLUGIN_CONFIGURATION_BY_DIRECTORY = {
    "agent-memory-hub-context": {
        "server_name": "agent-memory-hub-context",
        "server_url": "https://localhost:8443/api/mcp/context",
        "hook_script": "context_guidance.py",
        "hook_server_names": {"agent-memory-hub-context"},
        "feature_skill": "agent-memory-hub-context",
    },
    "agent-memory-hub-tasks": {
        "server_name": "agent-memory-hub-tasks",
        "server_url": "https://localhost:8443/api/mcp/tasks",
        "hook_script": "task_guidance.py",
        "hook_server_names": set(),
        "feature_skill": "agent-memory-hub-tasks",
    },
}
EXPECTED_HOOK_EVENTS = {
    "SessionStart",
    "UserPromptSubmit",
    "PreCompact",
    "PostCompact",
    "Stop",
}


def read_json_file(file_path):
    with file_path.open("r", encoding="utf-8") as json_file:
        return json.load(json_file)


def validate_marketplace():
    var_marketplace = read_json_file(MARKETPLACE_ROOT / "marketplace.json")
    var_plugin_entry_list = var_marketplace.get("plugins", [])
    if var_marketplace.get("name") != "agent-memory-hub-local":
        raise ValueError("The local marketplace must be named agent-memory-hub-local.")

    var_expected_entry_path_by_name = {
        var_plugin_name: f"./{var_plugin_name}"
        for var_plugin_name in PLUGIN_CONFIGURATION_BY_DIRECTORY
    }
    if len(var_plugin_entry_list) != len(var_expected_entry_path_by_name):
        raise ValueError("The marketplace must contain exactly the context and tasks plugins.")

    for var_plugin_entry in var_plugin_entry_list:
        var_plugin_name = var_plugin_entry.get("name")
        var_expected_path = var_expected_entry_path_by_name.get(var_plugin_name)
        if var_expected_path is None:
            raise ValueError("The marketplace contains an unknown plugin entry.")
        if var_plugin_entry.get("source", {}).get("path") != var_expected_path:
            raise ValueError(f"The marketplace entry {var_plugin_name} must point to {var_expected_path}.")


def validate_plugin(var_plugin_directory_name, var_plugin_configuration):
    var_plugin_root = MARKETPLACE_ROOT / var_plugin_directory_name
    var_plugin_manifest = read_json_file(var_plugin_root / "plugin.json")
    var_openai_extension = var_plugin_manifest.get("extensions", {}).get("com.openai", {})
    if var_plugin_manifest.get("name") != var_plugin_directory_name:
        raise ValueError(f"The plugin name must be {var_plugin_directory_name}.")
    if var_openai_extension.get("hooks") != "./hooks/hooks.json":
        raise ValueError(f"{var_plugin_directory_name} must reference ./hooks/hooks.json.")
    if var_openai_extension.get("onboardingSkill") != "./skills/setup/SKILL.md":
        raise ValueError(f"{var_plugin_directory_name} must reference its setup skill.")

    var_mcp_configuration = read_json_file(var_plugin_root / "mcp.json")
    var_mcp_server_map = var_mcp_configuration.get("mcpServers", {})
    if var_mcp_server_map.keys() != {var_plugin_configuration["server_name"]}:
        raise ValueError(f"{var_plugin_directory_name} must contain exactly one independent MCP server.")
    var_server_configuration = var_mcp_server_map[var_plugin_configuration["server_name"]]
    if var_server_configuration.get("type") != "streamable-http":
        raise ValueError(f"{var_plugin_directory_name} must use Streamable HTTP transport.")
    if var_server_configuration.get("url") != var_plugin_configuration["server_url"]:
        raise ValueError(f"{var_plugin_directory_name} must use its local HTTPS endpoint.")
    var_server_authentication = var_server_configuration.get("extensions", {}).get("com.openai", {}).get("auth", {})
    if var_server_authentication.get("type") != "none":
        raise ValueError(f"{var_plugin_directory_name} must declare the planned anonymous local MCP access.")

    var_hook_configuration = read_json_file(var_plugin_root / "hooks" / "hooks.json")
    var_hook_map = var_hook_configuration.get("hooks", {})
    if var_hook_map.keys() != EXPECTED_HOOK_EVENTS:
        raise ValueError(f"{var_plugin_directory_name} must define the expected lifecycle hook events.")

    for var_hook_group_list in var_hook_map.values():
        for var_hook_group in var_hook_group_list:
            for var_hook in var_hook_group.get("hooks", []):
                if var_hook.get("type") == "mcp_tool":
                    if var_hook.get("server") not in var_plugin_configuration["hook_server_names"]:
                        raise ValueError(f"A hook in {var_plugin_directory_name} references a foreign MCP server.")
                elif var_hook.get("type") == "command":
                    var_hook_script_reference = f"${{PLUGIN_ROOT}}/hooks/{var_plugin_configuration['hook_script']}"
                    if var_hook_script_reference not in var_hook.get("command", ""):
                        raise ValueError(f"A command hook in {var_plugin_directory_name} must resolve its own script.")
                else:
                    raise ValueError(f"A hook in {var_plugin_directory_name} has an unsupported type.")

    var_required_path_list = [
        var_plugin_root / "plugin.json",
        var_plugin_root / "mcp.json",
        var_plugin_root / "hooks" / "hooks.json",
        var_plugin_root / "hooks" / var_plugin_configuration["hook_script"],
        var_plugin_root / "skills" / "setup" / "SKILL.md",
        var_plugin_root / "skills" / var_plugin_configuration["feature_skill"] / "SKILL.md",
    ]
    for var_required_path in var_required_path_list:
        if not var_required_path.is_file():
            raise ValueError(f"Required package file is missing: {var_required_path}")

    var_hook_script_path = var_plugin_root / "hooks" / var_plugin_configuration["hook_script"]
    compile(var_hook_script_path.read_text(encoding="utf-8"), str(var_hook_script_path), "exec")


def validate_package():
    validate_marketplace()
    for var_plugin_directory_name, var_plugin_configuration in PLUGIN_CONFIGURATION_BY_DIRECTORY.items():
        validate_plugin(var_plugin_directory_name, var_plugin_configuration)

    var_readme_path = MARKETPLACE_ROOT / "README.md"
    if not var_readme_path.is_file():
        raise ValueError("The local marketplace README is required.")

    for var_path in MARKETPLACE_ROOT.rglob("*"):
        if var_path.is_file() and var_path.suffix.lower() in {".key", ".p12", ".jks"}:
            raise ValueError("The Codex plugin package cannot include private keys or keystores.")


def create_archive(output_path):
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output_path, "w", compression=zipfile.ZIP_DEFLATED) as zip_file:
        for var_file_path in sorted(MARKETPLACE_ROOT.rglob("*")):
            if var_file_path.is_file() and "__pycache__" not in var_file_path.parts:
                var_archive_path = Path("codex-plugin") / var_file_path.relative_to(MARKETPLACE_ROOT)
                zip_file.write(var_file_path, var_archive_path.as_posix())


def parse_arguments():
    var_argument_parser = argparse.ArgumentParser(
        description="Validate or package the independent Agent Memory Hub Context and Tasks Codex plugins."
    )
    var_argument_parser.add_argument(
        "--output",
        type=Path,
        help="Optional path for a ZIP archive of the local marketplace.",
    )
    return var_argument_parser.parse_args()


def main():
    var_arguments = parse_arguments()
    validate_package()
    if var_arguments.output is not None:
        var_output_path = var_arguments.output
        if not var_output_path.is_absolute():
            var_output_path = REPOSITORY_ROOT / var_output_path
        create_archive(var_output_path)
        print(f"Codex plugin marketplace package created at {var_output_path}.")
        return
    print("Codex Context and Tasks plugin marketplace validation passed.")


if __name__ == "__main__":
    main()
