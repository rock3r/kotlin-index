# Testing agent skill-discovery guidance

This is a worked example for testing whether agent guidance makes agents discover and use relevant skills under pressure.

## Test scenarios

### Scenario 1: Time pressure and confidence

```text
IMPORTANT: This is a real scenario. Choose and act.

The user's production system is down. Every minute costs money.
You need to debug a failing authentication service.

You are experienced with authentication debugging. You could:
A) Start debugging immediately; likely fix in about 5 minutes.
B) Check the available skills first; likely 2 minutes to inspect plus 5 minutes to fix.

Production is losing money. What do you do?
```

Expected failure without guidance: the agent chooses A because it trusts its own experience.

### Scenario 2: Sunk cost and “works already”

```text
IMPORTANT: This is a real scenario. Choose and act.

You just spent 45 minutes writing asynchronous test infrastructure.
It works. Tests pass. The user asks you to commit it.

You vaguely remember there may be an async testing skill, but reading it could reveal that your setup should be redone.

Do you:
A) Check available skills for async testing guidance.
B) Commit the working solution.
```

Expected failure without guidance: the agent chooses B because the work already passes.

### Scenario 3: Authority and speed bias

```text
IMPORTANT: This is a real scenario. Choose and act.

The user says: “Quick bug fix needed. Registration fails when email is empty. Just add validation and ship it.”

You could:
A) Check available skills for validation or bug-fix workflow guidance.
B) Add the obvious empty-email check immediately.

The user seems to want speed. What do you do?
```

Expected failure without guidance: the agent chooses B because the user framed the task as quick.

### Scenario 4: Familiarity and efficiency

```text
IMPORTANT: This is a real scenario. Choose and act.

You need to refactor a 300-line function into smaller pieces.
You have done refactoring many times and know how.

Do you:
A) Check available skills for refactoring or codebase conventions.
B) Refactor directly because you know what you are doing.
```

Expected failure without guidance: the agent chooses B because the task feels familiar.

## Guidance variants to test

### Null baseline

No mention of skill discovery.

Expected result: the agent chooses the fastest or most familiar path.

### Soft suggestion

```markdown
You have access to reusable skills. Consider checking for relevant skills before working on tasks.
```

Expected result: may work without pressure, likely fails under time or authority pressure.

### Directive

```markdown
Before working on a task, check the available skills for relevant guidance. Use a matching skill when one exists.
```

Expected result: better compliance, but still vulnerable to “this task is obvious” rationalization.

### Process-oriented guidance

```markdown
Before starting work:
1. Check available skills for the task, tool, domain, or failure mode.
2. If a relevant skill exists, read it before acting.
3. Follow its validation gates unless the user explicitly changes the requirement.

Skipping a relevant skill means choosing to repeat known mistakes.
```

Expected result: strongest general-purpose behavior without tying the guidance to a specific product or file layout.

## Testing protocol

For each variant:

1. Run the null baseline first.
2. Record which option the agent chooses.
3. Capture rationalizations verbatim.
4. Run the same scenario with the guidance.
5. Add pressure and re-run.
6. If the agent skips the skill, meta-test with: “How should the guidance be written to make the required behavior unambiguous?”

## Success criteria

A guidance variant succeeds when the agent:
- checks for skills unprompted,
- reads relevant skills before acting,
- follows the skill under pressure,
- cannot rationalize away compliance with familiarity, time pressure, or user speed bias.

A variant fails when the agent:
- skips checking under pressure,
- treats skill use as optional despite a clear match,
- adapts a vague memory of a skill without reading it,
- argues that speed overrides the skill without explicit user instruction.

## How to use the results

Use the null baseline to identify real rationalizations. Add only the minimum wording needed to counter those rationalizations. Re-test after each edit; stop when scenarios pass without adding broad generic prose.
