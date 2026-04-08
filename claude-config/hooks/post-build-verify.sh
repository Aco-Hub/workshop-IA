#!/usr/bin/env bash
# Post-build verification hook
# Verifies UI completeness: responsive check reminder, console error check, test coverage.

set -euo pipefail

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Only trigger after Angular build/serve commands
if ! echo "$COMMAND" | grep -qP '(ng serve|ng build|npm start|npm run build)'; then
  exit 0
fi

echo "UI DEFINITION OF DONE REMINDER:"
echo "  - [ ] Responsive: verified at 375px mobile width"
echo "  - [ ] At least one automated test confirms main user action"
echo "  - [ ] No console errors during mount/unmount"
echo "  - [ ] Interactive elements have :hover, :active, :focus styles"
echo "  - [ ] All buttons/links have data-testid attributes"
echo ""
echo "Run 'npm test' to verify before declaring this feature complete."

exit 0
