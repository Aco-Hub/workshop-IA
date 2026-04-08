#!/usr/bin/env bash
# Post-edit code review hook
# Reads JSON from stdin (PostToolUse event), checks the edited file for violations.
# Output goes back to Claude as feedback to self-correct.

set -euo pipefail

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

if [ -z "$FILE_PATH" ] || [ ! -f "$FILE_PATH" ]; then
  exit 0
fi

VIOLATIONS=""

# ─── Java checks ───
if [[ "$FILE_PATH" == *.java ]]; then

  # 1. Brace enforcement: if/for/while/else must use { }
  BRACE_ISSUES=$(grep -nP '^\s*(if|for|while|else)\s*(\(.*\))?\s*[^{;/]*$' "$FILE_PATH" 2>/dev/null | grep -vP '\{' | head -5 || true)
  if [ -n "$BRACE_ISSUES" ]; then
    VIOLATIONS+="BRACE STYLE: if/for/while/else must always use { }. Fix these lines:
$BRACE_ISSUES

"
  fi

  # 2. Final keyword on method parameters
  METHOD_PARAMS=$(grep -nP '(public|private|protected|static)\s+\w+\s+\w+\s*\(' "$FILE_PATH" 2>/dev/null || true)
  NON_FINAL_PARAMS=$(echo "$METHOD_PARAMS" | grep -vP '(final\s+\w+\s+(final\s+)?\w+|^\s*$|\(\s*\))' | grep -P '\(\s*[^)]+\)' | grep -vP 'final' | head -5 || true)
  if [ -n "$NON_FINAL_PARAMS" ]; then
    VIOLATIONS+="FINAL PARAMS: Method parameters should be final. Check these methods:
$NON_FINAL_PARAMS

"
  fi

  # 3. Non-final local variables (simple check for common patterns)
  NON_FINAL_VARS=$(grep -nP '^\s+(String|int|long|boolean|List|Map|Set|var)\s+\w+\s*=' "$FILE_PATH" 2>/dev/null | grep -vP '^\s+final\s' | head -5 || true)
  if [ -n "$NON_FINAL_VARS" ]; then
    VIOLATIONS+="FINAL VARS: Local variables should be final when possible. Check these:
$NON_FINAL_VARS

"
  fi

  # 4. Check for non-static imports that are commonly used statically
  STATIC_CANDIDATES=$(grep -nP '^import\s+(?!static\s)(org\.springframework\.http\.HttpStatus|java\.util\.Collections|java\.util\.stream\.Collectors|org\.junit\.jupiter\.api\.Assertions)' "$FILE_PATH" 2>/dev/null | head -5 || true)
  if [ -n "$STATIC_CANDIDATES" ]; then
    VIOLATIONS+="STATIC IMPORTS: These imports should be static:
$STATIC_CANDIDATES

"
  fi

  # 5. Check for missing Lombok annotations (handwritten getters/setters)
  HANDWRITTEN_GETTERS=$(grep -nP '^\s+public\s+\w+\s+get[A-Z]\w*\(\)\s*\{' "$FILE_PATH" 2>/dev/null | head -3 || true)
  if [ -n "$HANDWRITTEN_GETTERS" ]; then
    CLASS_HAS_LOMBOK=$(grep -P '@(Getter|Data|Value)' "$FILE_PATH" 2>/dev/null || true)
    if [ -z "$CLASS_HAS_LOMBOK" ]; then
      VIOLATIONS+="LOMBOK: Use @Getter/@Setter/@Data instead of handwritten accessors:
$HANDWRITTEN_GETTERS

"
    fi
  fi

  # 6. Check entity returned directly from controller (should be DTO)
  if echo "$FILE_PATH" | grep -qP 'Controller\.java$'; then
    ENTITY_RETURNS=$(grep -nP 'ResponseEntity<(?!.*Dto|.*DTO|.*Response|.*String|.*Void|.*Map|.*List|.*byte|.*Resource|.*\?)' "$FILE_PATH" 2>/dev/null | head -3 || true)
    if [ -n "$ENTITY_RETURNS" ]; then
      VIOLATIONS+="DTO PATTERN: Controllers must return DTOs, not entities. Check:
$ENTITY_RETURNS

"
    fi
  fi

  # 7. Check mapper not in controller layer
  if echo "$FILE_PATH" | grep -qP '(service|repository)/.*Mapper\.java$'; then
    VIOLATIONS+="MAPPER LOCATION: DTO mappers should be in the controller layer, not service/repository.

"
  fi
fi

# ─── TypeScript checks ───
if [[ "$FILE_PATH" == *.ts ]] && [[ "$FILE_PATH" != *.spec.ts ]]; then

  # 1. Import ordering: external imports first, blank line, then project imports
  IMPORT_BLOCK=$(grep -nP '^import\s' "$FILE_PATH" 2>/dev/null || true)
  if [ -n "$IMPORT_BLOCK" ]; then
    FOUND_PROJECT=false
    FOUND_EXTERNAL_AFTER=false
    while IFS= read -r line; do
      if echo "$line" | grep -qP "from\s+'(\.|@app|@core|@shared|@features|src/)"; then
        FOUND_PROJECT=true
      elif echo "$line" | grep -qP "from\s+'[^.]" && [ "$FOUND_PROJECT" = true ]; then
        FOUND_EXTERNAL_AFTER=true
        break
      fi
    done <<< "$IMPORT_BLOCK"
    if [ "$FOUND_EXTERNAL_AFTER" = true ]; then
      VIOLATIONS+="IMPORT ORDER: External imports must come before project imports (with a blank line between them).

"
    fi
  fi

  # 2. Private attributes must start with _
  PRIVATE_NO_UNDERSCORE=$(grep -nP '^\s+private\s+(?:readonly\s+)?(?!_)\w+' "$FILE_PATH" 2>/dev/null | grep -vP '(private\s+(readonly\s+)?(static|constructor|get |set |async ))' | head -5 || true)
  if [ -n "$PRIVATE_NO_UNDERSCORE" ]; then
    VIOLATIONS+="PRIVATE PREFIX: Private attributes must start with _. Check:
$PRIVATE_NO_UNDERSCORE

"
  fi

  # 3. Methods without explicit return type
  MISSING_RETURN=$(grep -nP '^\s+(public\s+|private\s+|protected\s+)?\w+\s*\([^)]*\)\s*\{' "$FILE_PATH" 2>/dev/null | grep -vP '(:\s*\w+|constructor|ngOn|ngAfter|ngDo)' | head -5 || true)
  if [ -n "$MISSING_RETURN" ]; then
    VIOLATIONS+="RETURN TYPE: Methods should have explicit return types. Check:
$MISSING_RETURN

"
  fi

  # 4. Readonly enforcement
  NON_READONLY=$(grep -nP '^\s+private\s+_\w+' "$FILE_PATH" 2>/dev/null | grep -vP 'readonly' | head -5 || true)
  if [ -n "$NON_READONLY" ]; then
    VIOLATIONS+="READONLY: Private attributes should be readonly when possible. Check:
$NON_READONLY

"
  fi
fi

# ─── SCSS checks ───
if [[ "$FILE_PATH" == *.scss ]]; then

  # 1. Check for parent selectors after children (simplified check)
  # This is hard to lint perfectly with grep, but we can check basic patterns

  # 2. Check nesting depth (max 3 levels recommended)
  DEEP_NESTING=$(grep -nP '^\s{8,}\S' "$FILE_PATH" 2>/dev/null | head -3 || true)
  if [ -n "$DEEP_NESTING" ]; then
    VIOLATIONS+="SCSS NESTING: Deeply nested selectors detected (4+ levels). Consider flattening:
$DEEP_NESTING

"
  fi
fi

# ─── Angular template checks ───
if [[ "$FILE_PATH" == *.html ]]; then

  # 1. Hardcoded user-facing strings (no i18n)
  HARDCODED=$(grep -nP '>[\s]*[A-Za-z]{3,}[^<]*</' "$FILE_PATH" 2>/dev/null | grep -vP '(translate|i18n|\{\{.*\|.*translate|<!--)' | head -5 || true)
  if [ -n "$HARDCODED" ]; then
    VIOLATIONS+="I18N: Possible hardcoded strings without i18n/translate pipe. Check:
$HARDCODED

"
  fi

  # 2. Non-semantic HTML
  DIV_BUTTONS=$(grep -nP '<div[^>]*(click|onClick)' "$FILE_PATH" 2>/dev/null | head -3 || true)
  if [ -n "$DIV_BUTTONS" ]; then
    VIOLATIONS+="SEMANTIC HTML: Use <button> instead of <div> with click handlers:
$DIV_BUTTONS

"
  fi

  # 3. Missing data-testid on interactive elements
  INTERACTIVE_NO_TESTID=$(grep -nP '<(button|a|input|select|textarea)\b' "$FILE_PATH" 2>/dev/null | grep -vP 'data-testid' | head -5 || true)
  if [ -n "$INTERACTIVE_NO_TESTID" ]; then
    VIOLATIONS+="TEST IDS: Interactive elements should have data-testid attributes:
$INTERACTIVE_NO_TESTID

"
  fi
fi

# ─── Output violations ───
if [ -n "$VIOLATIONS" ]; then
  echo "CODE REVIEW VIOLATIONS in $FILE_PATH:"
  echo ""
  echo "$VIOLATIONS"
  echo "Please fix these violations before proceeding."
fi

exit 0
