# Agent skill best practices

## Use this reference when

Read this when drafting or reviewing a skill's structure, trigger description, progressive disclosure, or validation plan.

## Core principles

### Keep the skill lean

Only include guidance the agent is likely to miss. Skills share context with the user's task, project instructions, and other skills. Every paragraph should earn its keep.

Ask:
- Does the agent need this, or is it generic knowledge?
- Is this a real local convention, gotcha, command, or validation gate?
- Could this be a reference file loaded only for a narrower case?

### Set the right degree of freedom

Match instruction strictness to task fragility:

- **High freedom**: exploratory review, writing, design, judgment-heavy analysis.
- **Medium freedom**: preferred patterns with contextual variation.
- **Low freedom**: fragile commands, mutating workflows, security-sensitive steps, or compliance rules.

Use exact commands only where the exact command matters. Otherwise, state the default and explain why.

### Ground skills in evidence

Strong skills are built from:
- completed task transcripts,
- observed agent failures,
- user corrections,
- review comments,
- incidents,
- runbooks,
- existing working scripts or commands.

Generic best-practice summaries rarely change behavior. Specific gotchas do.

## Frontmatter

Required fields:

```yaml
---
name: concise-hyphenated-name
description: Use when [specific triggering situations, symptoms, or user intents]
---
```

Guidance:
- Use lowercase kebab-case names.
- Keep descriptions specific and trigger-oriented.
- Include both what the skill does and when to use it.
- Avoid embedding a whole workflow in the description.
- Keep descriptions agent-neutral unless the skill intentionally targets a single harness.

## Progressive disclosure

Use the file system to avoid loading everything at once:

```text
skill-name/
  SKILL.md
  references/
  scripts/
  assets/
  evals/
```

Patterns:
- Put the common path and key gotchas in `SKILL.md`.
- Put optional or bulky detail in `references/`.
- Put deterministic helpers in `scripts/`.
- Put templates and static source material in `assets/`.
- Tell the agent exactly when to read or run each support file.

Keep references one level deep from `SKILL.md` where possible. If a reference grows beyond roughly 100 lines, add a table of contents.

## What to include

Include:
- specific workflow steps,
- validation gates,
- known failure modes,
- exact commands for fragile operations,
- output templates,
- environment assumptions,
- pointers to support files.

Avoid:
- generic advice,
- long background essays,
- redundant explanations,
- vendor-specific wording in portable skills,
- unsupported tool assumptions,
- too many equally weighted options.

## Validation loops

For quality-critical skills, add a feedback loop:

1. Produce an intermediate artifact.
2. Validate it with a script, checklist, or independent review.
3. Fix specific failures.
4. Re-run validation until clean.
5. Only then finalize.

Prefer executable validation for machine-checkable properties. Use checklist review for subjective or judgment-heavy work.

## Evals

Create evals before adding a lot of prose. Evals should answer: does this skill change behavior?

Good evals are:
- realistic,
- specific,
- pressure-tested when discipline matters,
- objectively checkable where possible,
- paired with baseline behavior when practical.

For mutating workflows, make evals plan-only unless the user authorizes real mutation.

## Common anti-patterns

- Writing a skill before observing the failure it should prevent.
- Filling `SKILL.md` with things the model already knows.
- Hiding trigger conditions in the body instead of the description.
- Adding many options without a recommended default.
- Mentioning one agent product in a skill meant to be portable.
- Assuming a browser, CLI, MCP, or package manager exists without saying so.
- Letting agents repeatedly reinvent the same brittle helper instead of bundling a script.

## Final checklist

Before sharing a skill, verify:

- [ ] Description is trigger-oriented and under the platform limit.
- [ ] `SKILL.md` contains the common path and critical gotchas only.
- [ ] Optional details are in references with clear read conditions.
- [ ] Commands and paths are portable or intentionally scoped.
- [ ] Evals or validation checks exist for testable behavior.
- [ ] No unintended vendor- or harness-specific language leaks into user-facing text.
