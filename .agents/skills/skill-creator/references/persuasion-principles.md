# Persuasion principles for skill design

## Use this reference when

Read this when a skill needs agents to follow a quality, safety, review, or process rule even when a shortcut looks faster.

## Overview

Agents often respond to the same textual cues that work in human process documentation: authority, commitment, social proof, urgency, and shared goals. Use these techniques ethically: the skill should serve the user's genuine interests and prevent predictable failures, not manipulate the user or agent into unrelated behavior.

## Principles

### Authority

Use clear imperatives for non-negotiable practices.

Good for:
- safety rules,
- destructive operations,
- TDD or review gates,
- security-sensitive workflows.

Example:

```markdown
Before editing production code, write the failing test. If production code was written first, revert it and restart from the test.
```

Avoid weak phrasing such as “consider” when the rule is actually mandatory.

### Commitment

Make the agent commit to the workflow before pressure appears.

Patterns:
- announce which skill is being used,
- copy a checklist into working notes,
- record red/green validation results,
- state the chosen path before acting.

This reduces silent skipping.

### Scarcity and immediacy

Use timing language when deferral is the failure mode.

Example:

```markdown
Run validation immediately after generating the plan, before applying changes.
```

Do not invent false urgency. Use immediacy only when ordering matters.

### Social proof

State reliable failure patterns plainly.

Example:

```markdown
Checklists that are not tracked get skipped. Keep the checklist visible until every item is complete.
```

This is useful when the skill counters a recurring agent habit.

### Unity

Use collaborative language when the task needs honest technical partnership rather than obedience.

Example:

```markdown
Treat review feedback as a shared attempt to protect the codebase, not as a request to defend the patch.
```

### Avoid manipulative techniques

Do not use flattery, guilt, fake scarcity, or false authority. The test is simple: would this guidance still be appropriate if the user read and understood exactly why it was written?

## Matching tone to skill type

| Skill type | Good techniques | Avoid |
| --- | --- | --- |
| Discipline-enforcing | Authority, commitment, social proof | Flattery, vague flexibility |
| Safety/security | Authority, exact sequences, red flags | Open-ended menus |
| Collaborative review | Unity, commitment, evidence-first framing | Heavy-handed obedience language |
| Pure reference | Clarity and navigation | Persuasion for its own sake |

## Practical patterns

### Red flags

List phrases that indicate the agent is about to rationalize a shortcut.

```markdown
Red flags — stop and re-check the skill:
- “This is obvious.”
- “The user wants speed.”
- “I can validate manually.”
```

### Rationalization table

Use when evals reveal repeated excuses.

```markdown
| Rationalization | Reality |
| --- | --- |
| “Tests after cover the same cases.” | The skill is about design pressure and proof order, not only coverage. |
```

### Bright-line rule plus reason

```markdown
Do not merge until CI and required reviews are green. This prevents local confidence from overriding shared repository gates.
```

The reason helps the agent generalize without weakening the rule.

## Checklist

- [ ] Does the stronger wording protect the user's actual goal?
- [ ] Is the rule based on observed failures or high-risk domain knowledge?
- [ ] Are red flags concrete rather than moralizing?
- [ ] Is the skill still concise?
- [ ] Would the user be comfortable seeing this guidance?
