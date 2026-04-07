# Claude Code Workshop Setup

## Quick Start (macOS / Linux / Windows)

```bash
git clone <repo-url>
cd "workshop IA"
python setup-claude-code.py import
```

Then edit `~/.mcp.json` to add your own API keys (Figma, etc.) and restart Claude Code.

## What you get

- **3 MCP servers**: context7 (docs lookup), exa (web search), figma (design)
- **70+ VoltAgent subagents**: core-dev, lang, qa-sec, data-ai
- **2 Angular skills**: angular-developer, angular-new-app
- **Auto-allowed tools**: Bash, Read, Edit, Write, WebFetch
- **Effort level**: max

## Prerequisites

- Python 3.7+ (pre-installed on macOS/Linux, install from python.org on Windows)
- Node.js + npx (for MCP servers)
- Claude Code CLI

## Re-export after changes

If you update your Claude Code config and want to share the new version:

```bash
python setup-claude-code.py export
git add -A && git commit -m "update claude config"
git push
```
