# kotlin-code-index

Portable **persistent** local code index (Xodus + `.kotlin-index/`).  
**selection-context** is the first application: SelectionContainer / DisableSelection facts for Compose/Jewel UI audits.

Repo folder: `compose-selection-index` (rename optional).  
Upstream alignment: storage contracts compatible with in-app code-index epic #812 (#814, #818).

## New session?

Start at **[.plans/HANDOFF.md](.plans/HANDOFF.md)**.

## Docs

| Doc | Topic |
|-----|--------|
| [.plans/kotlin-code-index-core.md](.plans/kotlin-code-index-core.md) | Core platform |
| [.plans/application-selection-context.md](.plans/application-selection-context.md) | App #1 |
| [AGENTS.md](AGENTS.md) | Agent rules |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers |
| [docs/INDEX-STORAGE.md](docs/INDEX-STORAGE.md) | `.kotlin-index/` + keys |
| [docs/CLI.md](docs/CLI.md) | Commands |

## Build

```bash
./gradlew check
./gradlew shadowJar   # build/libs/*-all.jar
```

## Status

Scaffolding only — implement **Core C0** + **App A1** per master plan.
