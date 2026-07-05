# Conventions

## Code Style

- **No wildcard imports**
- **Line length ≤ 120 characters**
- **No magic numbers** — named constants for PSI depth limits, cache TTLs, etc.
- **Exception handling** — catch at CLI boundaries; return typed exit codes. Do not swallow
  exceptions in analysis core without attaching file/line context.
- **Method focus** — extract helpers when branching accumulates; keep walker pure where possible.

## File Placement

| What | Where |
|------|--------|
| CLI entry + subcommands | `src/main/kotlin/com/kotlincodeindex/cli/` |
| Selection PSI walk | `src/main/kotlin/com/kotlincodeindex/application/selectioncontext/` |
| Topology (Bazel, Gradle) | `src/main/kotlin/com/kotlincodeindex/topology/` (future) |
| Index + cache | `src/main/kotlin/com/kotlincodeindex/core/` |
| DTOs / report models | `src/main/kotlin/com/kotlincodeindex/application/selectioncontext/model/` |
| Kotlin PSI bootstrap | `src/main/kotlin/com/kotlincodeindex/parse/` |
| Fixture snippets | `src/test/resources/fixtures/` |
| Fixture-driven tests | `src/test/kotlin/com/kotlincodeindex/` |

## Git Workflow

- Do not push directly to `main` without confirming with the user.
- Run `./gradlew check` before push or PR.
- Branch names: `feat/bazel-topology`, `fix/walker-disable-selection`.
- Commit messages: imperative subject, no conventional-commit prefixes (`feat:`, `fix:`).
- Reference issues in PR descriptions when applicable.

## Pre-Push Checks

```bash
./gradlew test
./gradlew check          # tests + detekt + ktfmt
./gradlew ktfmtFormat    # apply ktfmt to main, test, and Gradle scripts
```

Add format/lint tasks here when introduced; `./gradlew check` runs tests, detekt, and ktfmt.
