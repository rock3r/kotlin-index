---
name: using-git-worktree
description: Use when starting feature work that needs isolation from current workspace or before executing implementation plans - creates isolated git worktrees with smart directory selection and safety verification
---

# Using Git Worktrees

## Overview

Git worktrees create isolated workspaces sharing the same repository, allowing work on multiple branches simultaneously without switching.

**Core principle:** Systematic directory selection + safety verification = reliable isolation.

**Announce at start:** "I'm using the using-git-worktrees skill to set up an isolated workspace."

## Pre-Flight: Already Isolated?

Before doing anything else, check whether the current session is already inside a worktree or on a non-main branch:

```bash
# Already inside a worktree?
if [ "$(git rev-parse --git-common-dir)" != "$(git rev-parse --git-dir)" ]; then
  echo "Already inside a worktree ($(pwd)) — skipping creation."
  # Report the existing worktree location and exit the skill
fi

# On a non-main feature branch? (skip if detached HEAD — that's not isolation)
current_branch=$(git rev-parse --abbrev-ref HEAD)
main_branch=$(git symbolic-ref refs/remotes/origin/HEAD 2>/dev/null | sed 's|refs/remotes/origin/||')
# Fallback: if origin/HEAD is not set, sed exits 0 with empty output, so default explicitly
main_branch="${main_branch:-main}"
if [ "$current_branch" != "HEAD" ] \
   && [ "$current_branch" != "$main_branch" ] \
   && [ "$current_branch" != "main" ] \
   && [ "$current_branch" != "master" ]; then
  echo "Already on feature branch '$current_branch' — skipping worktree creation."
  # Report the current branch and exit the skill
fi
```

**If either check is true:** Report that isolation already exists and skip the rest of this skill. Do not ask the user or create a new worktree.

## Directory Selection Process

Follow this priority order:

### 1. Check Existing Directories

```bash
# Check in priority order
ls -d .worktrees 2>/dev/null     # Preferred (hidden)
ls -d worktrees 2>/dev/null      # Alternative
```

**If found:** Use that directory. If both exist, `.worktrees` wins.

### 2. Check CLAUDE.md

```bash
grep -i "worktree.*director" CLAUDE.md 2>/dev/null
```

**If preference specified:** Use it without asking.

### 3. Ask User

If no directory exists and no CLAUDE.md preference, use `.worktrees/ (project-local, hidden)`.

## Safety Verification

### For Project-Local Directories (.worktrees or worktrees)

**MUST verify directory is ignored before creating worktree:**

```bash
# Check if directory is ignored (respects local, global, and system gitignore)
git check-ignore -q .worktrees 2>/dev/null || git check-ignore -q worktrees 2>/dev/null
```

**If NOT ignored:**

Per Jesse's rule "Fix broken things immediately":
1. Add appropriate line to .gitignore
2. Commit the change
3. Proceed with worktree creation

**Why critical:** Prevents accidentally committing worktree contents to repository.

## Creation Steps

### 1. Detect Project Name

```bash
project=$(basename "$(git rev-parse --show-toplevel)")
```

### 2. Choose Base Branch

Detect the current branch:

```bash
current_branch=$(git rev-parse --abbrev-ref HEAD)
main_branch=$(git symbolic-ref refs/remotes/origin/HEAD 2>/dev/null | sed 's|refs/remotes/origin/||' || echo "main")
```

**If on main/master:** Skip this step — the worktree will branch from the current HEAD.

**If on a feature branch:** Ask the user:

```
You're currently on '<current_branch>'. Which branch should the worktree start from?

1. Current branch (<current_branch>)
2. <main_branch> (main branch)
3. Other (specify)
```

In either case, make sure the chosen branch is up to date with the remote. If not, pull/rebase it first.
This will ensure the worktree is based on the latest changes and avoid conflicts.

Use the chosen branch as the base for `git worktree add`:

```bash
# Branch from chosen base
git worktree add "$path" -b "$NEW_BRANCH_NAME" "$BASE_BRANCH"
```

### 3. Create Worktree

```bash
# Determine full path
case $LOCATION in
  .worktrees|worktrees)
    path="$LOCATION/$BRANCH_NAME"
    ;;
  ~/.config/superpowers/worktrees/*)
    path="~/.config/superpowers/worktrees/$project/$BRANCH_NAME"
    ;;
esac

# Create worktree with new branch from chosen base
git worktree add "$path" -b "$BRANCH_NAME" "$BASE_BRANCH"
cd "$path"
```

> **Note:** In agent harnesses where each shell call is isolated, `cd` in one call does not
> affect the next. Use absolute paths for file tools, or prefix bash commands with
> `cd "/absolute/path/to/worktree" && …`.

### 3. Run Project Setup

Auto-detect and run appropriate setup:

```bash
# JVM / Gradle
if [ -f gradlew ]; then ./gradlew classes; fi

# Node.js
if [ -f package.json ]; then npm install; fi

# Rust
if [ -f Cargo.toml ]; then cargo build; fi

# Python
if [ -f requirements.txt ]; then pip install -r requirements.txt; fi
if [ -f pyproject.toml ]; then poetry install; fi

# Go
if [ -f go.mod ]; then go mod download; fi
```

### 4. Verify Clean Baseline

Run tests to ensure worktree starts clean:

```bash
# Examples - use project-appropriate command
npm test
cargo test
pytest
go test ./...
```

**If tests fail:** Report failures, ask whether to proceed or investigate.

**If tests pass:** Report ready.

### 5. Report Location

```
Worktree ready at <full-path>
Tests passing (<N> tests, 0 failures)
Ready to implement <feature-name>
```

## Quick Reference

| Situation | Action |
|-----------|--------|
| Already inside a worktree | Skip — report existing worktree and exit skill |
| Already on a feature branch | Skip — report current branch and exit skill |
| `.worktrees/` exists | Use it (verify ignored) |
| `worktrees/` exists | Use it (verify ignored) |
| Both exist | Use `.worktrees/` |
| Neither exists | Check CLAUDE.md → Ask user |
| Directory not ignored | Add to .gitignore + commit |
| On main/master | Branch from current HEAD without asking |
| On feature branch | Ask: current branch / main / other |
| Tests fail during baseline | Report failures + ask |
| No package.json/Cargo.toml | Skip dependency install |

## Common Mistakes

### Skipping ignore verification

- **Problem:** Worktree contents get tracked, pollute git status
- **Fix:** Always use `git check-ignore` before creating project-local worktree

### Assuming directory location

- **Problem:** Creates inconsistency, violates project conventions
- **Fix:** Follow priority: existing > CLAUDE.md > ask

### Proceeding with failing tests

- **Problem:** Can't distinguish new bugs from pre-existing issues
- **Fix:** Report failures, get explicit permission to proceed

### Hardcoding setup commands

- **Problem:** Breaks on projects using different tools
- **Fix:** Auto-detect from project files (package.json, etc.)

## Example Workflow

```
You: I'm using the using-git-worktrees skill to set up an isolated workspace.

[Check .worktrees/ - exists]
[Verify ignored - git check-ignore confirms .worktrees/ is ignored]
[Create worktree: git worktree add .worktrees/auth -b feature/auth]
[Run npm install]
[Run npm test - 47 passing]

Worktree ready at /Users/jesse/myproject/.worktrees/auth
Tests passing (47 tests, 0 failures)
Ready to implement auth feature
```

## Red Flags

**Never:**
- Create worktree without verifying it's ignored (project-local)
- Skip baseline test verification
- Proceed with failing tests without asking
- Assume directory location when ambiguous
- Skip CLAUDE.md check

**Always:**
- Follow directory priority: existing > CLAUDE.md > ask
- Verify directory is ignored for project-local
- Auto-detect and run project setup
- Verify a clean test baseline

## Gradle in Worktrees

When working in a worktree, verify `./gradlew test` or `./gradlew check` from the worktree
path before pushing. Gradle wrapper and cache paths are usually worktree-local.

## Cleanup and Failure Recovery

After a branch is merged or abandoned, clean up from the **main checkout**, not from inside the
worktree being removed:

```bash
cd /absolute/path/to/main/checkout && git worktree list --porcelain
cd /absolute/path/to/main/checkout && git worktree remove /absolute/path/to/worktree
cd /absolute/path/to/main/checkout && git branch -D <branch-name>
```

If cleanup fails, diagnose the failure before retrying:

| Failure | Likely cause | Recovery |
|---------|--------------|----------|
| `fatal: '<path>' is not a working tree` | Git no longer has the path registered, but the directory still exists | From the main checkout, confirm `git worktree list --porcelain` does not list it, then remove the stale directory (`rm -rf <path>`) |
| `'<path>' contains modified or untracked files` | The worktree still has local changes | Inspect with `git -C <path> status --short`; only use `git worktree remove --force <path>` if those changes are intentionally disposable |
| `cannot delete branch ... checked out at <path>` | The branch is still attached to a registered worktree | Remove the worktree first, then delete the branch from the main checkout |
| Follow-up shell commands fail with `spawn bash ENOENT` or similar | The agent/session was still anchored in the directory that was just deleted | Use a fresh command that starts with `cd /absolute/path/to/main/checkout && ...`; if the harness remains stuck, report that the stale directory was removed and ask the user to start the next task from the main checkout |

Never run cleanup commands from inside the worktree you are deleting. Prefer explicit
main-checkout commands for every cleanup step.

## Integration

**Called by:**
- Any skill needing isolated workspace
