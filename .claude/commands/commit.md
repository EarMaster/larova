---
description: Commit recent changes using a conventional commit message, after syncing with the remote.
allowed-tools: Bash(git status:*), Bash(git pull:*), Bash(git merge:*), Bash(git diff:*), Bash(git add:*), Bash(git commit:*), Bash(git push:*), AskUserQuestion, Edit, Write
model: haiku
---
Commit recent changes using a conventional commit message, after syncing with the remote.

## Steps

1. **Check git status** — run `git status` to see staged, unstaged, and untracked files. If there is nothing staged, stage recent changes. If there are unstaged or changed files from previous sessions group them (if possible) and use `AskUserQuestion` to ask the user if these changes shall be committed as well.

2. **Sync** — run `git pull --rebase` to incorporate upstream changes before committing. If it fails (e.g. conflicts), stop and report the error; do not proceed.

3. **Analyze the diff** — run `git diff --cached` to understand what is staged.

4. **Draft a conventional commit message** following this format:
   ```
   <type>(<optional scope>): <short description>

   <optional body — explain WHY, not WHAT>
   ```
   - **type**: `feat` | `fix` | `docs` | `style` | `refactor` | `test` | `chore` | `perf` | `ci` | `build` | `revert`
   - **scope**: affected module/component (omit if unclear or cross-cutting). Larova's usual scopes track the module layout in `AGENTS.md`: `home`, `card`, `help`, `transfer`, `settings`, `domain`, `data`, `ui`, `platform`, plus `release`, `store` and `l10n`.
   - **short description**: imperative mood, lowercase, no trailing period, ≤ 72 chars
   - **body**: wrap at 72 chars; omit if the subject line is self-explanatory
   - Add `BREAKING CHANGE: <description>` in the footer if applicable

5. **Update CHANGELOG.md** — prepend an entry for this commit under the `[Unreleased]` section (create the file and section if absent), but **only if the change is user-facing** — a new feature, fix, or behaviour change someone using Larova would notice. Skip this step for CI/CD, docs-only, or internal tooling changes (see AGENTS.md's "Conventions"). Format:
   ```
   ## [Unreleased]

   ### <Category>
   - <human-readable summary of the change>
   ```
   Categories: `Added` | `Changed` | `Fixed` | `Removed` | `Security` | `Performance`. Use one category block per entry; add multiple bullets if needed. Stage `CHANGELOG.md` with `git add CHANGELOG.md` before committing.

6. **Check the invariants** — before confirming, re-read the numbered invariants in `AGENTS.md` and check the staged diff against them. These are the mistakes that cannot be repaired once users own data, and a commit is the last cheap moment to catch one. In particular: a hex colour or bitmap where a token key belongs, a hardcoded user-facing string, `left`/`right` instead of `start`/`end`, amber or alarm red offered as a tile colour, anything that adds a network dependency, or anything that interprets user content. If the diff trips one, say so and stop rather than committing it.

7. **Confirm** — use `AskUserQuestion` with two questions in a single call:
   1. Show the drafted commit message and CHANGELOG entry (or note that none was needed); ask the user to approve or request changes.
   2. Ask whether to push after committing.

8. **Commit** — once confirmed, run `git commit -m "<message>"`.

9. **Merge** — run `git merge --no-edit origin/main` to pull in any commits that landed on `main` (e.g. PR merge commits) so this branch stays up-to-date with the base branch; skip this if already on `main`. If it fails, stop and report the error.

10. **Push** — if the user chose to push, run `git push`. Note: `main` represents the released state and is branch-protected (PR required, enforced for admins) — a direct `git push` to `main` will be rejected. Work lands on `develop` or a branch off it; never open a PR targeting `main` (see AGENTS.md's "Branching model").
