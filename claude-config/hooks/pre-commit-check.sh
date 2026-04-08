#!/usr/bin/env bash
# Pre-commit quality gate hook
# Runs tests and verification before allowing a commit.

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ERRORS=""

# ─── Backend tests ───
if [ -d "$PROJECT_ROOT/backend" ]; then
  echo "Running backend tests..."
  if ! (cd "$PROJECT_ROOT/backend" && ./gradlew test --quiet 2>&1 | tail -5); then
    ERRORS+="BACKEND TESTS FAILED. Fix test failures before committing.
"
  fi
fi

# ─── Frontend tests ───
if [ -d "$PROJECT_ROOT/frontend" ] && [ -f "$PROJECT_ROOT/frontend/package.json" ]; then
  echo "Running frontend tests..."
  if ! (cd "$PROJECT_ROOT/frontend" && npm test 2>&1 | tail -10); then
    ERRORS+="FRONTEND TESTS FAILED. Fix test failures before committing.
"
  fi
fi

# ─── Check for hardcoded strings in templates ───
HARDCODED_COUNT=$(grep -rP '>[\s]*[A-Za-z]{4,}[^<]*</' "$PROJECT_ROOT/frontend/src" --include="*.html" 2>/dev/null | grep -vcP '(translate|i18n|\{\{.*translate|<!--|data-testid)' || true)
if [ "$HARDCODED_COUNT" -gt 0 ]; then
  ERRORS+="I18N: Found $HARDCODED_COUNT possible hardcoded strings in HTML templates. Use translate pipe.
"
fi

# ─── Output ───
if [ -n "$ERRORS" ]; then
  echo "PRE-COMMIT CHECK FAILED:"
  echo ""
  echo "$ERRORS"
  exit 1
fi

echo "All pre-commit checks passed."
exit 0
