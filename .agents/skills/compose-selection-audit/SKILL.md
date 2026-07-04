---
name: compose-selection-audit
description: >
  Use when auditing Compose/Jewel UI for SelectionContainer context: interactive composables
  inside a SelectionContainer without DisableSelection, nested selection containers, or missing
  exclusion wrappers. Uses kotlin-code-index (selection-context application) for precomputed
  facts from a persistent `.kotlin-index/` index — not manual file reading. Bazel monorepos primary;
  Gradle secondary.
---

# Compose Selection Audit

Uses **kotlin-code-index** with `--application selection-context`. The skill applies **policy**;
the index supplies **facts** from `.kotlin-index/index/<commit>/` (persistent — build once, query many).

## Prerequisites

- Built fat JAR: `./gradlew shadowJar` → `build/libs/*-all.jar`
- Target repo path (Bazel monorepo root)
- Optional: `.bazelproject` or explicit `--bazel-target`

## Quick workflow

1. **Scope** — leaf UI Bazel target from `.bazelproject`.
2. **Index** (once per commit/scope — may take minutes on large repos):

   ```bash
   java -jar /path/to/kotlin-code-index-all.jar index \
     --project /path/to/target-repo \
     --bazel-target //plugins/foo/ui:ui \
     --include-deps \
     --applications selection-context
   ```

3. **Query** (fast — reads Xodus):

   ```bash
   java -jar /path/to/kotlin-code-index-all.jar query \
     --project /path/to/target-repo \
     --application selection-context \
     --preset interactive-in-sc \
     --format jsonl
   ```

4. **Point query** for a diff line:

   ```bash
   java -jar /path/to/kotlin-code-index-all.jar query \
     --project /path/to/target-repo \
     --application selection-context \
     --file relative/path/Panel.kt \
     --line 142 \
     --format json
   ```

5. **Status** if unsure index is fresh:

   ```bash
   java -jar /path/to/kotlin-code-index-all.jar status --project /path/to/target-repo
   ```

## Policy table

| Callee kind | `inSelectionContainer` | `excludedByDisableSelection` | Verdict |
|-------------|------------------------|------------------------------|---------|
| ActionButton, IconButton, clickable Row/Box | true | false | **Violation** |
| Text, Markdown (selectable) | true | — | OK (informational) |
| Interactive control | true | true | OK |
| Any | true, `selectionContainerCount > 1` | — | **Warning** |

## Output shape

```markdown
## Selection container findings
1. [high] ActionButton in SC without DisableSelection — `Panel.kt:142`

## Index
- store: `.kotlin-index/index/<commit>/`
- application: selection-context
- confidence: lexical
```

## Rules

- Run `index` before batch `query` if `status` shows stale or missing manifest
- Do not re-read source files for SC nesting when index rows exist
- Add `.kotlin-index/` to target repo gitignore if not present

## Integration

Run before broader `compose-ui-audit` for token efficiency.
