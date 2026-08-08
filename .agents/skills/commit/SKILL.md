---
name: commit
description: Create one reviewed Conventional Commit and push it for VaultNote only after the user explicitly requests a commit or push.
---

# VaultNote commit and push

Use this skill only after an explicit user command such as `коміт`, `commit`, or
`коміт і пуш`. One invocation performs the complete operation: inspect, validate,
stage, commit, and push. Never invoke it proactively.

## Workflow

1. Check the current branch, upstream, status, and diff. If the current branch
   is `main`, derive a short kebab-case topic from the complete change and run
   `git switch -c <type>/<topic>` (for example, `feat/user-registration`).
   Continue only after confirming the new branch. Stop on a detached HEAD or
   an unresolved merge/rebase state and ask the user to resolve it.
2. Review all current changes. In this repository all non-ignored changes are
   intentionally in scope, so stage them with `git add -A`; do not hide or
   discard changes. Git-ignored files (for example local credentials and
   `.mcp.json`) remain excluded by Git.
3. Run the relevant quality gate before staging the final commit. For backend
   changes run `./gradlew check` from `backend/`; include focused checks when
   they provide faster feedback. Stop on a failed check and report the cause.
4. Run `git diff --cached --check` and inspect the staged summary and diff.
   Do not continue if the staged content contains secrets or generated output.
5. Choose a concise Conventional Commit message (`feat`, `fix`, `docs`,
   `build`, `ci`, `refactor`, `test`, or `chore`) that describes the complete
   staged change. Use imperative English and keep the subject short.
6. Run `git commit -m "<message>"` and allow the native pre-commit hook to
   execute. Never bypass hooks with `--no-verify`.
7. Only after a successful commit, push the current branch with `git push` (or
   `git push -u origin <branch>` when no upstream exists). Do not force-push.
8. After a successful push, include a pull-request creation URL for the current
   branch. Derive it from the `origin` GitHub remote when possible.
9. If the commit succeeds but push fails, report the commit hash and exact push
   error without retrying blindly. Do not amend or create another commit unless
   the user explicitly asks.

## Completion report

Report the branch, commit hash and message, files included, checks run, and
whether the push succeeded. When the push succeeds, include the pull-request
creation link. Keep the report concise and in Ukrainian when the conversation
is in Ukrainian. Updating `docs/project-progress.md` is separate: do it only
when the user explicitly asks to update progress.
