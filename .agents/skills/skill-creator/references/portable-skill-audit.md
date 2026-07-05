# Portable and bundled skill audit

## Use this reference when

Read this when creating or editing a bundled/exported skill, or when turning a repo-local workflow into a skill that may be exported to another project.

## Bundled skill classes

Use a `skills-bundle.json` manifest to distinguish two classes:

- `include`: bundled/exported skills. These ship with the product repo and can be exported into other projects, so treat them as portable skills.
- `exclude`: non-bundled repo-local maintainer skills. These can reference repository docs, local validation commands, and maintainer-only workflows.

A product/domain-specific bundled skill may still name Jewel, Spectre, or another product when that product is the domain of the task. The audit is about avoiding accidental harness or checkout assumptions.

## Audit checklist for bundled/exported skills

- Avoid hardcoded local checkout paths such as `/Users/...`, `~/src/...`, or repo-specific scratch paths.
- Avoid hardcoded harness install folders such as `~/.agents`, `~/.claude`, or `.codex` unless the skill intentionally targets that harness.
- Prefer installed-skill placeholders in command examples:

```bash
SKILL_PATH=/path/to/installed/skill
TOOL="python3 $SKILL_PATH/scripts/tool.py"
```

- Put real runtime requirements in frontmatter `compatibility` when they affect use.
- Keep support files self-contained from the exported skill directory's perspective.
- If a skill relies on helper scripts, validate script entrypoints from the exported or symlinked location.
- Do not tell agents to broaden sandbox or network access as a generic workaround.
- Remove generated artifacts before finalizing: `__pycache__/`, `*.pyc`, eval scratch, media outputs, release bundles, and planning scratch.
- If static validation can enforce a rule, add validation rather than relying only on prose.

## Mutation safety

For portable skills that operate remote systems:

- Do read-only discovery before writes.
- Require direct user authorization for the specific target and action.
- Verify the concrete target ID, URL, field values, reviewer, label, topic, or artifact path before mutation.
- After mutation, run read-only verification and summarize the changed state.
- Never ask users to paste cookie values, bearer tokens, XSRF tokens, copied curl commands containing secrets, or browser-exported credential jars.

## Source metadata

When folding portable guidance into a skill, update `skill-source.json` in the same change. Record canonical repository provenance, not the local checkout path used for inspection. If a derivative source itself incorporated guidance from another source and you closely adapt that material, record both sources.
