#!/usr/bin/env python3
"""
Claude Code Config - Export & Import (cross-platform: macOS, Linux, Windows)

Usage:
    python setup-claude-code.py export   -> packs your config into claude-config/
    python setup-claude-code.py import   -> restores config from claude-config/
"""
import json
import os
import platform
import re
import shutil
import subprocess
import sys
from pathlib import Path


def get_claude_dir() -> Path:
    """~/.claude on all platforms (Claude Code uses this everywhere)."""
    return Path.home() / ".claude"


def get_agents_dir() -> Path:
    return Path.home() / ".agents"


def get_mcp_json() -> Path:
    return Path.home() / ".mcp.json"


def get_export_dir() -> Path:
    return Path(__file__).resolve().parent / "claude-config"


CONFIG_FILES = [
    "settings.json",
    "settings.local.json",
]

# Regex to redact API keys/secrets/tokens in JSON values
SECRET_PATTERN = re.compile(
    r'("(?:[^"]*(?:_KEY|_SECRET|_TOKEN|_PASSWORD|API_KEY|SECRET_KEY))"'
    r'\s*:\s*)"[^"]+"',
    re.IGNORECASE,
)


def redact_secrets(text: str) -> str:
    return SECRET_PATTERN.sub(r'\1"YOUR_VALUE_HERE"', text)


def copy_tree(src: Path, dst: Path):
    """Copy directory tree, resolving symlinks."""
    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(src, dst, symlinks=False)


def backup_file(path: Path):
    if path.exists():
        bak = path.with_suffix(path.suffix + ".bak")
        shutil.copy2(path, bak)
        print(f"  [backup] {path} -> {bak.name}")


# ── Export ────────────────────────────────────────────────────

def export_config():
    claude_dir = get_claude_dir()
    agents_dir = get_agents_dir()
    mcp_json = get_mcp_json()
    export_dir = get_export_dir()

    export_dir.mkdir(parents=True, exist_ok=True)
    print("=== Exporting Claude Code config ===\n")

    # 1. Config files
    for name in CONFIG_FILES:
        src = claude_dir / name
        if src.is_file():
            shutil.copy2(src, export_dir / name)
            print(f"  [copied] {src}")

    # 2. MCP servers (redact secrets)
    if mcp_json.is_file():
        text = mcp_json.read_text(encoding="utf-8")
        (export_dir / "mcp.json").write_text(
            redact_secrets(text), encoding="utf-8"
        )
        print(f"  [copied] {mcp_json} (secrets redacted)")

    # 3. Skills (resolve symlinks)
    skills_src = None
    if (agents_dir / "skills").is_dir():
        skills_src = agents_dir / "skills"
    elif (claude_dir / "skills").is_dir():
        skills_src = claude_dir / "skills"

    if skills_src and any(skills_src.iterdir()):
        dst = export_dir / "skills"
        copy_tree(skills_src, dst)
        count = sum(1 for p in dst.iterdir() if p.is_dir())
        print(f"  [copied] {skills_src} ({count} skills)")

    # 4. Hooks
    hooks_json = claude_dir / "hooks.json"
    if hooks_json.is_file():
        shutil.copy2(hooks_json, export_dir / "hooks.json")
        print("  [copied] hooks.json")

    hooks_dir = claude_dir / "hooks"
    if hooks_dir.is_dir():
        copy_tree(hooks_dir, export_dir / "hooks")
        print("  [copied] hooks/")

    # 5. Installed plugins manifest
    plugins_json = claude_dir / "plugins" / "installed_plugins.json"
    if plugins_json.is_file():
        shutil.copy2(plugins_json, export_dir / "installed_plugins.json")
        print("  [copied] installed_plugins.json")

    # 6. Known marketplaces
    markets_json = claude_dir / "plugins" / "known_marketplaces.json"
    if markets_json.is_file():
        shutil.copy2(markets_json, export_dir / "known_marketplaces.json")
        print("  [copied] known_marketplaces.json")

    # 7. Agents
    agents_defs = claude_dir / "agents"
    if agents_defs.is_dir() and any(agents_defs.iterdir()):
        copy_tree(agents_defs, export_dir / "agents")
        print("  [copied] agents/")

    print(f"\n=== Export complete: {export_dir} ===")
    print("\nReview claude-config/mcp.json and fill in any redacted API keys.")
    print("Commit the claude-config/ folder to share your setup.")


# ── Import ────────────────────────────────────────────────────

def import_config():
    claude_dir = get_claude_dir()
    agents_dir = get_agents_dir()
    mcp_json = get_mcp_json()
    export_dir = get_export_dir()

    if not export_dir.is_dir():
        print(f"ERROR: {export_dir} not found. Nothing to import.")
        sys.exit(1)

    claude_dir.mkdir(parents=True, exist_ok=True)
    print("=== Importing Claude Code config ===\n")

    # 1. Config files
    for name in CONFIG_FILES:
        src = export_dir / name
        dst = claude_dir / name
        if src.is_file():
            backup_file(dst)
            shutil.copy2(src, dst)
            print(f"  [restored] {dst}")

    # 2. MCP config
    src_mcp = export_dir / "mcp.json"
    if src_mcp.is_file():
        backup_file(mcp_json)
        shutil.copy2(src_mcp, mcp_json)
        print(f"  [restored] {mcp_json}")
        text = mcp_json.read_text(encoding="utf-8")
        if "YOUR_VALUE_HERE" in text:
            print("  !! WARNING: ~/.mcp.json contains placeholder API keys. Edit them before use!")

    # 3. Skills
    skills_src = export_dir / "skills"
    if skills_src.is_dir() and any(skills_src.iterdir()):
        skills_dst = agents_dir / "skills"
        skills_dst.mkdir(parents=True, exist_ok=True)
        for skill in skills_src.iterdir():
            if skill.is_dir():
                target = skills_dst / skill.name
                if target.exists():
                    shutil.rmtree(target)
                shutil.copytree(skill, target)

        # Create symlinks in ~/.claude/skills/
        claude_skills = claude_dir / "skills"
        claude_skills.mkdir(parents=True, exist_ok=True)
        for skill in skills_dst.iterdir():
            if skill.is_dir():
                link = claude_skills / skill.name
                if not link.exists():
                    try:
                        link.symlink_to(skill)
                    except OSError:
                        # Windows without dev mode: just copy instead
                        shutil.copytree(skill, link)
        print("  [restored] skills -> ~/.agents/skills/")

    # 4. Hooks
    hooks_json = export_dir / "hooks.json"
    if hooks_json.is_file():
        shutil.copy2(hooks_json, claude_dir / "hooks.json")
        print("  [restored] hooks.json")

    hooks_dir = export_dir / "hooks"
    if hooks_dir.is_dir():
        copy_tree(hooks_dir, claude_dir / "hooks")
        print("  [restored] hooks/")

    # 5. Agents
    agents_src = export_dir / "agents"
    if agents_src.is_dir():
        agents_dst = claude_dir / "agents"
        agents_dst.mkdir(parents=True, exist_ok=True)
        for item in agents_src.iterdir():
            target = agents_dst / item.name
            if item.is_dir():
                if target.exists():
                    shutil.rmtree(target)
                shutil.copytree(item, target)
            else:
                shutil.copy2(item, target)
        print("  [restored] agents/")

    # 6. Install plugins
    plugins_manifest = export_dir / "installed_plugins.json"
    if plugins_manifest.is_file():
        print("\n  Installing plugins...")

        # Restore marketplace config first
        markets_src = export_dir / "known_marketplaces.json"
        if markets_src.is_file():
            plugins_dir = claude_dir / "plugins"
            plugins_dir.mkdir(parents=True, exist_ok=True)
            shutil.copy2(markets_src, plugins_dir / "known_marketplaces.json")
            print("  [restored] known_marketplaces.json")

        # Read plugin names
        data = json.loads(plugins_manifest.read_text(encoding="utf-8"))
        plugin_names = list(data.get("plugins", {}).keys())

        # Find claude CLI
        claude_cmd = shutil.which("claude")
        if claude_cmd and plugin_names:
            for plugin in plugin_names:
                print(f"  -> Installing plugin: {plugin}")
                try:
                    subprocess.run(
                        [claude_cmd, "plugin", "install", plugin],
                        check=True,
                        capture_output=True,
                        text=True,
                    )
                except (subprocess.CalledProcessError, FileNotFoundError):
                    print(f"     !! Failed. Install manually: claude plugin install {plugin}")
        else:
            if not claude_cmd:
                print("  -> Claude CLI not found. Install plugins manually:")
            for p in plugin_names:
                print(f"       claude plugin install {p}")

    print("\n=== Import complete ===")
    print("\nNext steps:")
    print("  1. Edit ~/.mcp.json and fill in your API keys")
    print("  2. Restart Claude Code")


# ── Main ──────────────────────────────────────────────────────

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in ("export", "import"):
        print(f"Usage: python {sys.argv[0]} {{export|import}}")
        print()
        print("  export  - Copy your Claude Code config into claude-config/")
        print("  import  - Restore config from claude-config/ to your system")
        sys.exit(1)

    if sys.argv[1] == "export":
        export_config()
    else:
        import_config()
