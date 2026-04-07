# Workshop IA - AI Coding Setup

Shared configuration for Claude Code and GitHub Copilot.

## What you get

- **3 MCP servers**: context7 (docs lookup), exa (web search), figma (design)
- **7 custom agents**: angular-architect, python-pro, java-architect, search-specialist, typescript-pro, devops-engineer, backend-developer
- **2 Angular skills** (Claude Code only): angular-developer, angular-new-app
- **Project instructions**: architecture decisions, testing conventions

## Prerequisites

- Node.js + npx (for MCP servers)
- Python 3.7+ (for Claude Code import script)

## GitHub Copilot Setup

1. Clone the repo:
   ```bash
   git clone <repo-url>
   cd "workshop IA"
   ```

2. Open the folder in VS Code. Copilot automatically picks up:
   - `.vscode/mcp.json` (MCP servers)
   - `.github/copilot-instructions.md` (project instructions)
   - `.github/agents/` (7 custom agents)

3. Edit `.vscode/mcp.json` and replace `YOUR_FIGMA_API_KEY` with your actual key.

4. Open Copilot Chat in **Agent mode** to use MCP tools and custom agents.

## Claude Code Setup

1. Clone the repo:
   ```bash
   git clone <repo-url>
   cd "workshop IA"
   python setup-claude-code.py import
   ```

2. Edit `~/.mcp.json` and replace `YOUR_VALUE_HERE` with your actual API keys.

3. Restart Claude Code.

## Re-export Claude Code config

If you update your Claude Code config and want to share the new version:

```bash
python setup-claude-code.py export
git add -A && git commit -m "update claude config"
git push
```
