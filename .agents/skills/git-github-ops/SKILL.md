---
name: git-github-ops
description: >
  Handle git and GitHub workflows end-to-end: summarize diffs into high-quality
  commit messages, create commits non-interactively, push branches, prepare PR
  titles/descriptions, and operate GitHub via gh CLI with file-based inputs to
  avoid shell escaping bugs. Use whenever a task includes commit, push, PR
  creation/updates, review replies, merge prep, or gh automation.
allowed-tools: Read, Write, Edit, Grep, Find, LS, Bash(git *), Bash(gh *)
---

# Git + GitHub Ops

Use this skill when the user asks for git/GitHub execution, not just code edits.

The goals are:
1. Make accurate, specific commit/PR text from real diffs.
2. Keep shell commands simple and non-interactive.
3. Avoid quoting/escaping breakage by using temp files.
4. Respect `AGENTS.md` policy gates for PR open/merge actions.

## Operating rules

- Use `gh` CLI for GitHub operations.
- Prefer non-interactive commands and explicit flags.
- Keep one command = one purpose.
- For long/multiline text, **never inline it inside a bash string**.
- Write text to temp files and pass file paths to git/gh commands.
- Never add a `Labels` section to issue body markdown. Manage labels only via
  GitHub labels (`--label`, `gh issue edit --add-label`, etc.).
- Follow `AGENTS.md` policy for actions needing explicit user approval
  (`gh pr create`, `gh pr merge`, etc.).

## Commit message policy

When summarizing diffs into a commit message, follow this contract exactly:

- Focus on **specific changes visible in the diffs**.
- Do not invent IDs, bug numbers, tickets, names, or emails.
- Do not include PII unless it is explicitly present in the diff and required.
- **Skip prefixes.** No conventional-commits-style typed prefixes
  (`feat(area):`, `fix(...):`, `chore(...):`, `docs(area):`,
  `refactor(...):`), and no generic ones (`chore:`, `bug:`, `misc:`).
  When a single area or file is the focus, lead with that bare name
  instead — `<area-or-file>: <what changed>` reads cleaner and tells
  reviewers at a glance what the commit touched. Otherwise just write
  the imperative subject directly.
- Use canonical format:
  - Subject line: max 72 chars
  - Empty line
  - Body wrapped to max 72 chars per line

### Commit message shape

```text
<imperative subject, <= 72 chars>

- <specific change 1>
- <specific change 2>
- <specific change 3>
```

Keep the body concrete and diff-derived (files changed, behavior changed,
validation added, docs updated).

## Commit workflow

1. Inspect staged diff (`git diff --staged`).
2. Draft commit text in a temp file (or start from
   `templates/commit-message.txt`).
3. Commit with file input, not inline `-m` chains.

Example:

```bash
# write with the Write tool
# /tmp/commit-msg.txt

git commit -F /tmp/commit-msg.txt
```

If the commit needs edits, rewrite the file and run `git commit --amend -F ...`.

## PR title format

PR title should be specific and review-friendly:

```text
<area>: <what changed and why it matters>
```

Rules:
- Keep to ~72 chars when possible.
- Name the touched area (`agent`, `permissions`, `ui`, `tests`, etc.).
- State outcome, not process.
- No vague titles like `fix stuff` or `updates`.

Examples:
- `skills: add git/github workflow with file-based gh commands`
- `rpc: preserve tool call order when merging adjacent outputs`

## PR description format

Create PR body as Markdown in a temp file, then pass via `--body-file`.
You can start from `templates/pr-description.md`.

Template:

```markdown
## Summary
- What changed
- Why this is needed

## Testing
- [x] <test or check command>
- [x] <manual validation>

## Risks
- Any regression risk or migration impact

## Scope
- In scope: <what this PR intentionally covers>
- Out of scope: <what is intentionally excluded>
```

For UI work, add a `## Screenshots` section with links/paths.

## gh CLI: file-first patterns (important)

To avoid escaping issues, do not stuff multiline content into bash strings.
Use temp files and pass paths.

### Create PR

```bash
gh pr create \
  --title "<single-line title>" \
  --body-file /tmp/pr-body.md \
  --base main \
  --head <branch>
```

### PR comment / review reply

```bash
gh pr comment <pr-number> --body-file /tmp/pr-comment.md
```

### Issue comment

```bash
gh issue comment <issue-number> --body-file /tmp/issue-comment.md
```

### Optional: avoid title escaping too (JSON input path)

When title text is hard to quote safely, write full payload to a file and call
`gh api --input`.

```bash
gh api repos/<owner>/<repo>/pulls \
  --method POST \
  --input /tmp/create-pr.json
```

JSON payload file shape:

```json
{
  "title": "skills: add git/github workflow with file-based gh commands",
  "head": "git-github-ops-skill",
  "base": "main",
  "body": "<markdown body text>"
}
```

## Filing GitHub issues

Use labels consistently. List current labels before applying any:

```bash
gh label list --repo <owner>/<repo>
```

Create issues with file-based bodies:

```bash
gh issue create \
  --repo <owner>/<repo> \
  --title "..." \
  --body-file /tmp/issue-body.md \
  --label "bug"
```

When the user asks to start work on an issue, add a `wip` label if the repo uses one
(validate it exists first):

```bash
gh issue edit <issue-number> \
  --repo <owner>/<repo> \
  --add-label "wip"
```

For large efforts, prefer an epic issue plus linked sub-issues rather than one oversized
issue body.

## Suggested execution sequence

1. `git status --short`
2. `git diff --staged`
3. Build commit message file and commit with `git commit -F`
4. `git push --set-upstream origin <branch>`
5. Build PR body markdown file
6. `gh pr create --body-file ...`
7. If asked to monitor: hand over to `babysit-pr`

## Safety checks before push/PR

- Ensure scope matches user request (no unrelated changes).
- Confirm required tests/checks are green for the task.
- Confirm no policy violations from `AGENTS.md`.
- Before opening/merging PR, ensure explicit user approval exists.

## Handoff format to user

When done, report:
- commit subject(s)
- PR number/link (if created)
- tests/checks run
- any pending approvals needed
