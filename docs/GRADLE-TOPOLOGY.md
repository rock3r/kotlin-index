# Gradle Topology (Phase 2)

Secondary project-discovery backend for Gradle-based Compose/Jewel repos (plugin projects,
smaller open-source trees). **Bazel remains primary** for IntelliJ Platform and Android Studio
monorepos.

## Detection

Auto-select Gradle when no `MODULE.bazel` / `WORKSPACE` at `--project` root and
`settings.gradle(.kts)` exists. Override with `--build-system gradle`.

## Module discovery

1. Parse `settings.gradle.kts` / `settings.gradle` for included projects
2. Supplement with `.idea/modules.xml` + `*.iml` when present (AS/IDEA import metadata)
3. Map `--gradle-module :foo:ui` to source roots:
   - `src/main/kotlin`
   - KMP: `src/commonMain/kotlin`, `src/jvmMain/kotlin`, `src/androidMain/kotlin`

## Dependency closure

- Parse `build.gradle.kts` for `project(":…")` dependencies (in-repo only)
- `--include-deps` walks transitive project deps; ignore Maven coordinates
- Leaf module heuristic: no other included module depends on it (optional auto-detect)

## CLI flags (Phase 2)

```bash
compose-selection-index index \
  --project . \
  --build-system gradle \
  --gradle-module :plugin:ui \
  [--include-deps]
```

## Cache

Same `.compose-selection-index/` layout as Bazel; key includes Gradle module path + hash of
relevant `build.gradle.kts` files.

## Limits

- No Gradle invocation required (parse settings/build files only)
- Optional `./gradlew projects` subprocess for validation (external, not a plugin)
- Does not resolve version-catalog-only dynamic source sets without filesystem walk fallback

Same cache format and CLI JSONL output as Bazel path; backend tag `gradle-parse` in rows.

Full implementation: [.plans/phase-2-gradle-topology.md](../.plans/phase-2-gradle-topology.md) (phase **G** in [master-plan.md](../.plans/master-plan.md)).
