# Testing

## Expectations

1. **Unit tests for analysis logic** — walker, callee matching, exclusion rules, nested SC
   counting. Use fixture `.kt` files under `src/test/resources/fixtures/`.
2. **Unit tests for topology** — `.bazelproject` parsing, BUILD snippet parsing (no live Bazel
   required in CI).
3. **Edge cases** — nested SC, DisableSelection between site and SC, no SC, import aliases,
   markdown `selectable = true` when configured.
4. Run `./gradlew test` before considering a change complete.

## TDD Red-Green Cycle

1. Write the test first.
2. Run targeted test; confirm **assertion failure** (not merely compile error).
3. Minimum implementation.
4. Confirm green.
5. Full `./gradlew test`.

```bash
./gradlew test --tests "com.selectionindex.analysis.SelectionWalkerTest.nested selection containers"
```

Do not skip the red step.

## Fixture Layout

```
src/test/resources/fixtures/
  nested-selection-containers.kt
  disable-selection-in-sc.kt
  no-selection-container.kt
  import-alias-selection-container.kt
```

Fixtures are minimal `@Composable` snippets — valid Kotlin syntax, no external deps required
for parse-only tests.

## Testing Anti-Patterns

**Do not** read production source from disk in tests and assert `source.contains("token")`.

**Do not** test Kotlin/PSI behavior by substring-matching generated walker output strings
without structural assertions on `SelectionContext` fields.

| Claim | Right tool |
|-------|------------|
| "call site is inside SC" | Parse fixture → locate call at line → assert `inSelectionContainer` |
| "excluded by DisableSelection" | Fixture with SC + DS + button → assert `excludedByDisableSelection` |
| "two SCs on path" | Nested SC fixture → assert `selectionContainerCount == 2` |
| "Bazel query returns these files" | Mock process output or golden-file parsed BUILD |

## Live Bazel Tests

Optional `@Tag("bazel")` tests that require `bazel` on PATH and a fixture workspace. Excluded from
default `./gradlew test`; run explicitly when developing topology:

```bash
./gradlew test --tests "*BazelTopology*" -PbazelTests
```

Keep CI green without Bazel installed.

## Test Completeness

Before marking work done, verify every scenario in the plan has a test that was red then green.
