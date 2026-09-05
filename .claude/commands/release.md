---
description: Bump the app version (major/minor/patch or explicit semver), update CHANGELOG.md and the per-locale Play Store release notes, and commit the release.
allowed-tools: Bash(git status:*), Bash(git pull:*), Bash(git diff:*), Bash(git add:*), Bash(git commit:*), Bash(git push:*), Read, Edit, Bash, AskUserQuestion, TodoWrite, TodoRead
model: haiku
---

You are preparing a new release of the Larova Android app. Your job is to determine the new version number, update `app/build.gradle.kts`, restructure `CHANGELOG.md`, generate the per-locale Play Store release notes under `fastlane/metadata/android/`, verify their character limits, and commit the result.

## Step 0 — Check readiness

Larova is documentation and seed files until M0 lands the Gradle project — see `AGENTS.md` and `docs/implementation-plan.md`. If `app/build.gradle.kts` does not exist yet, stop and tell the user this command cuts an actual app release and needs the `:app` module first; there is nothing to version yet.

## Input

The user may have provided an argument: `$ARGUMENTS`

Interpret `$ARGUMENTS` as follows:
- `major`, `minor`, or `patch` (case-insensitive) → bump that semver component
- A full semver string like `1.0.4` or `2.0.0` → use it exactly
- Empty / blank → analyse the changelog and suggest a bump type (see below)
- Anything else → it is unusual; flag it and ask for confirmation or correction

## Step 1 — Read current version

Read `app/build.gradle.kts` and extract:
- `versionName` (e.g. `"0.1.0"`)
- `versionCode` (integer)

## Step 2 — Determine target version

**If argument is `major` / `minor` / `patch`:**
Compute the new semver by incrementing the appropriate component (reset lower components to 0).

**If argument is a valid semver (`X.Y.Z` with all three numeric parts):**
Use it as-is. Warn (but don't block) if the new version is less than or equal to the current one.

**If argument is empty:**
Read the `## [Unreleased]` section of `CHANGELOG.md`. Based on the entries:
- Any `### Added` → at minimum a `minor` bump
- Only `### Fixed` or `### Changed` (no new features) → `patch`
- Breaking change indicated (rare) → `major`

Formulate a suggestion with a one-sentence rationale.

**If argument is unusual** (e.g. only two parts like `1.2`, has leading zeros, non-numeric, etc.):
Do not proceed. Ask the user to confirm or correct.

## Step 3 — Confirm with user

Before making any changes, use **AskUserQuestion** to confirm. Show:
- Current version
- Proposed new version
- New versionCode (current + 1)

Single question, two options: "Proceed" and "Cancel / change". If the user cancels or provides a correction, re-evaluate from Step 2 with their input, then confirm again.

## Step 4 — Apply changes

**4a. Check the export schema version**

Larova's release is not only an APK — it is also a file format. If this release changed the export
container (`content.json` structure, a new `CardPayload` type, anything under `media/`), check that
`schemaVersion` in the manifest was bumped and that an older export still imports. An export
written by this version must be readable by it, and an export from a *newer* version must decline
politely rather than crash — see `docs/technical-notes.md` §6. A release that silently breaks a
user's backup file is the one release that cannot be walked back, because the file is the only copy
of their data.

State plainly which of the two applies: the format is unchanged, or it changed and the version was
bumped and round-tripped.

**4b. Create release-note (What's New) files**

Release notes live in the fastlane metadata tree, one file per locale, named after the **new versionCode** — not the versionName:

```
fastlane/metadata/android/{LOCALE}/changelogs/{NEW_VERSION_CODE}.txt
```

So bumping to versionCode 7 means creating `fastlane/metadata/android/en-US/changelogs/7.txt` and its thirteen siblings. Getting this wrong is the most likely mistake in this step: a file named `0.2.0.txt` or one using the *old* versionCode will simply never be picked up, and the release ships with no notes.

**All fourteen locales are mandatory.** There is no English fallback for store text — a locale with no file gets a blank What's New in the Play Store for that language. `google-play.yml` fails the deploy if any locale is missing a file for the versionCode being released, so an incomplete set blocks the release rather than shipping quietly:

`en-US` (source — write this one first, the rest are translations of it), `de-DE`, `fr-FR`, `it-IT`, `es-ES`, `pt-PT`, `uk`, `pl-PL`, `ru-RU`, `tr-TR`, `ar`, `hi-IN`, `zh-CN`, `ja-JP`.

Those are **Play locale codes**, which are not the language tags in `locales_config.xml`. Do not "correct" `zh-CN` to `zh-Hans`, or drop the region from `hi-IN` or `ja-JP`, or add one to `uk` or `ar` — the directory names as they stand are what the Play API accepts. Read the existing directory names rather than deriving them from `docs/localization.md`, and if a directory is missing, create it rather than skipping the locale.

Each file contains a short user-facing summary of the release. Aim for **max 300 characters in the English draft** to leave headroom for translation expansion; the hard limit is 500 per locale (see the check below). German runs about 30 % longer than English and French about 20 %, so a 400-character English note will not fit. Chinese and Japanese compress to roughly half, which is not a licence to write more there — keep the same note, not a longer one. Base the content on the `## [Unreleased]` section of `CHANGELOG.md`, but write it in plain language for the person who set the app up — not a technical log. Do not copy changelog bullet points verbatim.

Only include changes that are visible or relevant to someone using the app. Exclude anything related to CI/CD workflows, GitHub Actions, internal tooling, or other infrastructure — users don't see these.

**Register, not reach:** the audience for this text is a parent, and often a grandparent reading the store listing before installing. Say what changed in the words they would use. And keep it inside the product's boundary — Larova is a notebook that stores and displays, so release notes never describe it as advising, tracking, monitoring or interpreting anything about a child. That boundary is what keeps the app outside the EU MDR definition of a medical device (`docs/concept.md` §2.2), and store text is exactly where it gets crossed by accident.

**Translation quality:** per `docs/localization.md` §5, unreviewed machine translation is not acceptable for anything a user reads. Draft all fourteen yourself, then **state plainly, in the final message, which ones have not been read by a native speaker** — that is twelve of them by default, since only `en-US` and `de-DE` are maintained by the author. Do not present the set as finished translations. Store text deserves *more* care than in-app strings, not less: in-app strings fall back to English when missing, store text does not.

Fourteen locales is enough work that the temptation is to shorten the loop. Two things not to do: do not write English into a non-English file to fill the slot (a blank note is a visible gap; an English note in the Japanese listing looks like a bug and is one), and do not reuse the previous release's note for a locale you did not get to. Both pass the character check and both ship.

**4c. Verify the character limits**

Google Play's 500-character What's New limit is per locale and counted in characters. Translation length varies a lot by language, so an in-limit English draft guarantees nothing about the rest.

Run the validator with the **new** versionCode:

```
tools/check_store_metadata.sh {NEW_VERSION_CODE}
```

(Invoke it through `bash` — the repo is developed on Windows with `core.filemode=false`, so the script's executable bit is not recorded in git.)

It checks every store locale for a present, non-empty, in-limit file and exits non-zero listing every problem. If a locale is over, re-trim *that locale's* text (summarize or combine points) and re-run until it passes. Do not skip this because the English source was short, and do not hand-count instead: `wc -m` counts bytes rather than characters unless the shell locale is UTF-8, so on Windows it reports 4 for `für` and makes German look over-limit. The script counts correctly and is the only measurement to trust here.

Treat a non-zero exit as blocking — the same check runs in CI and will fail the deploy.

**4d. Update `app/build.gradle.kts`**

Replace the `versionCode` and `versionName` lines with the new values. Use Edit — do not rewrite the whole file.

**4e. Update `CHANGELOG.md`**

Get today's date via: `date +%Y-%m-%d`

Replace the line `## [Unreleased]` (at the top of the Unreleased section) with:

```
## [Unreleased]

## [X.Y.Z] - YYYY-MM-DD
```

This preserves an empty Unreleased section for future work and stamps the release with today's date.

**Do not hard-wrap the bullets.** Each bullet is a single line in the file, however long. GitHub
renders a release body with hard line breaks on, so every source newline becomes a `<br>`: a
hand-wrapped bullet reads as a paragraph broken mid-sentence on a phone, which is where most people
open a release. `CHANGELOG.md` is the one Markdown file in the repo with no column limit.

## Step 5 — Check the screenshot goldens

A release commit is the last chance to catch a stale golden before CI fails on it. The screenshot
tests compare the UI against committed PNGs, so any change to a Compose screen — a new row, a
reworded string, a spacing tweak — makes the matching golden wrong. Nothing in an ordinary build
catches this: `./gradlew test` only checks that every screen still composes, so a stale golden
passes locally right up until `ci.yml` runs `verifyRoborazziDebug`.

Larova's golden matrix covers every tile type across light, dark, night and 200 % font scale, so a
single UI change usually invalidates four images rather than one.

Work out whether that is likely for this release — did it touch the UI or any user-visible string?

```
git diff --stat $(git describe --tags --abbrev=0)..HEAD -- '**/ui/**' 'app/src/main/res/values/strings.xml' '**/screenshots/'
```

If UI or strings changed but the `screenshots/` folder did not, tell the user the goldens are
probably stale and confirm it:

```
./gradlew :app:verifyRoborazziDebug --tests '*ScreenshotTest'
```

On failure, look at the `_compare` images in `app/build/outputs/roborazzi/` and check the diff is
the intended change rather than a regression, then accept the new look:

```
./gradlew :app:recordRoborazziDebug --tests '*ScreenshotTest'
```

The `--tests` filter is not optional. Include the updated PNGs in the release commit.

## Step 6 — Commit

Run:
```
git add app/build.gradle.kts CHANGELOG.md fastlane/metadata/android/
git commit -m "chore(release): bump version to X.Y.Z"
```

Use `AskUserQuestion` to ask whether to push. If so run `git push`.

Note the branching model (`AGENTS.md`): this commit belongs on `develop`. `main` represents the
released state and is branch-protected, and it only advances by merging `develop` into it — which
is what triggers `release.yml` to tag, build and publish. **Promoting `develop` to `main` is the
maintainer's call, not something this command initiates.** Say that the release commit is ready and
stop there.
