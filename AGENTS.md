# AGENTS.md

Guidance for coding agents working in the Larova repository. This file is the
single source of truth; `CLAUDE.md` only points here.

## What this repository is

Larova is an **offline Android app** (Kotlin, Compose Multiplatform) that parents
set up on their child's phone: a grid of self-authored tiles — guides, notes,
checklists, tables, contacts, media — that a caregiver can read in two taps.
No account, no server, **no internet permission**.

**The project is currently documentation and seed files only.** There is no
Gradle build and no module structure yet. `docs/` is the design of record; `app/`
holds three files written ahead of the build to lock in decisions that are
expensive to reverse.

**Fixed identifiers.** Application ID `app.larova`; domain `larova.app` (the
reversed domain, so the ID is verifiably ours). Neither can change after the
first Play release. The domain's first job is the privacy policy URL Play
requires on the listing, `https://larova.app/privacy` — the one store asset an
offline app cannot supply for itself. It does not weaken the no-internet claim:
the app never resolves the domain, and has no permission with which to.
See `docs/technical-notes.md` §7.

**Store listing is translated into all fourteen app languages**, not just the
launch market's two — a caregiver who needs the per-app language picker will read
the listing in that language first. `en-US` is the source and `de-DE` is
author-maintained; the other twelve are model drafts awaiting a native
read-through, which must be said out loud rather than quietly shipped. Play's
locale codes are not the app's language tags (`zh-CN` not `zh-Hans`, `hi-IN`,
`ja-JP`, bare `uk` and `ar`) — read the directory names, don't derive them.

## Current state of the tree

| Path | Contents |
| --- | --- |
| `docs/concept.md` | Product, users, regulatory boundary, feature scope |
| `docs/technical-notes.md` | Stack, module layout, data model, export format, Android specifics |
| `docs/implementation-plan.md` | Milestones M0–M5 and their exit criteria |
| `docs/localization.md` | The fourteen launch languages and the rules around them |
| `docs/design/design-system.md` | Colour tokens, icon, typography, layout |
| `docs/design/prototypes/*.html` | Clickable references: screen flow, icon sheet, colour grid |
| `app/src/main/kotlin/app/larova/core/ui/theme/AppColors.kt` | The colour token table and mode resolution |
| `app/src/main/res/values/strings.xml` | English base strings — the single source |
| `app/src/main/res/xml/locales_config.xml` | Per-app language list |
| `brand/*.svg` | Adaptive icon layers and the store icon |
| `CHANGELOG.md` | [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) history. **Update `## [Unreleased]` in the same commit as any user-facing change.** `release.yml` extracts GitHub Release notes from the `## [x.y.z]` heading matching `versionName`, so headings must stay exact |
| `fastlane/metadata/android/` | Play Store listing text, one folder per store locale in `fastlane supply` layout. Written by `/release`, validated by `tools/check_store_metadata.sh`, consumed by `google-play.yml`. See that folder's `README.md` |
| `tools/check_store_metadata.sh` | Validates listing text against Play's per-locale character limits (title 30, short 80, full 4000, release notes 500) and listing images (24-bit PNG, no alpha, 320–3840 px a side, long side at most twice the short one, at least two per set). Pass a versionCode to also require release notes in every store locale |
| `.claude/commands/` | Repo slash commands: `/commit` and `/release` — see "Slash commands" |
| `docs/pages/` | The GitHub Pages site (Jekyll), deployed by `pages.yml` — landing page and privacy policy, served at `larova.app`. Built on the MIT-licensed Hydra template, vendored; see its `_config.yml` for what was stripped out and why |
| `docs/release-setup.md` | One-time signing and publishing setup: upload key, the four GitHub secrets, Play Console prerequisites, branch protection |

## Commands

There is **no Gradle wrapper in the tree yet**, so nothing builds or tests today.
Creating it is the first task of M0 (`docs/implementation-plan.md`). Do not
invent build commands until it exists; once it does, the intended entry points
are the standard ones:

```bash
./gradlew assembleDebug                        # build
./gradlew test                                 # unit tests, all modules
./gradlew :core:domain:test                    # unit tests, one module
./gradlew :core:domain:test --tests "*ExportRoundTripTest*"   # a single test
./gradlew connectedAndroidTest                 # instrumented tests, device/emulator
./gradlew lint detekt                          # Android Lint + Detekt
./gradlew verifyRoborazziDebug                 # screenshot comparison
./gradlew recordRoborazziDebug                 # re-record screenshot baselines
```

```bash
bash tools/check_store_metadata.sh          # listing text and images
bash tools/check_store_metadata.sh 7        # also require release notes for versionCode 7
```

The metadata check works today and does not need Gradle. Invoke it through `bash`: the repo is
developed on Windows with `core.filemode=false`, so the script's executable bit is not recorded
in git and running it by path can fail with exit 126.

## Branching model

`main` represents **the released state of the app** — nothing else. `develop` is where all
ongoing work lands first: commits, feature branches, PRs, all of it. `develop` is intentionally
left unprotected (direct pushes are fine — the maintainer is a solo developer and doesn't want
extra ceremony there). `main` only ever advances by merging `develop` into it at release time,
which is what triggers `release.yml`'s tag/build/publish.

**Never open a pull request targeting `main`.** Base every branch and PR on `develop` instead.
Promoting `develop` to `main` for a release is the maintainer's call to make, not something to
initiate unprompted — and it is now enforced rather than conventional: `main` requires a pull
request with admin enforcement on, so a direct push is refused whoever makes it. Signing keys,
secrets and that protection are documented in `docs/release-setup.md`.

Commits follow [Conventional Commits](https://www.conventionalcommits.org/): `type(scope):
description`, imperative and lowercase. Scopes track the module layout — `home`, `card`, `help`,
`transfer`, `settings`, `domain`, `data`, `ui`, `platform` — plus `release`, `store` and `l10n`.

## Release process

1. Work lands on `develop`, each user-facing change adding its own `## [Unreleased]` entry in
   `CHANGELOG.md`.
2. `/release [major|minor|patch|X.Y.Z]` bumps `versionName`/`versionCode` in
   `app/build.gradle.kts`, stamps the `Unreleased` section with the new version and today's date,
   writes per-locale Play release notes to
   `fastlane/metadata/android/{locale}/changelogs/{versionCode}.txt`, and validates them. It
   commits `chore(release): bump version to X.Y.Z` and stops — it does not promote to `main`.
3. The maintainer opens and merges a `develop` → `main` pull request. `main` is branch-protected
   with admin enforcement, so this is the only way it advances — a direct push is refused. This
   promotion PR is the one exception to "never open a PR targeting `main`", and it is the
   maintainer's to open, never an agent's.
4. `release.yml` reads `versionName`, tags `vX.Y.Z`, builds a signed APK and AAB, and cuts a
   GitHub Release with notes pulled from the matching `CHANGELOG.md` heading. It then calls
   `google-play.yml` for the `internal` track.

Two things about that chain are worth knowing before relying on it. **Release notes are named
after the `versionCode`, not the `versionName`** — `changelogs/7.txt` is for versionCode 7, and a
file named `1.2.0.txt` is silently never picked up. And **the release is a file format as much as
an APK**: if the export container changed, `schemaVersion` must have been bumped and an older
export must still import (`docs/technical-notes.md` §6). A broken export format is the one
regression that cannot be walked back, because that file may be the only copy of a family's data.

## CI/CD

`.github/workflows/`, ported from a sibling project and adapted:

- `ci.yml` / `codeql.yml` — build, unit tests, lint + Detekt, screenshot verification and CodeQL
  analysis on PRs to `main` or `develop`. **On pull requests only** — a direct push to `develop`
  runs neither, so work pushed straight to that branch has had no build, no lint and no golden
  verification until it reaches a PR. Both start with a `detect` job gated on a root `./gradlew`,
  which does not exist yet, so **every job currently passes by skipping**. `ci.yml`'s screenshot
  job has a second gate on a `*ScreenshotTest*.kt` existing, and reports a golden mismatch as a
  warning rather than a failure — read the comment in the file for what that costs.
- `release.yml` — tags `main` from `app/build.gradle.kts`'s `versionName`, builds a signed
  APK/AAB, and cuts a GitHub Release with notes from `CHANGELOG.md`. Gated on
  `app/build.gradle.kts` existing. Needs `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
  `KEY_PASSWORD` repo secrets before it can sign anything.
- `pages.yml` — deploys `docs/pages/` via Jekyll to GitHub Pages on push to `main`, path-filtered
  to `docs/pages/**`. Pages must be enabled in repo settings with source "GitHub Actions", the
  custom domain set there, and `larova.app` DNS pointed at GitHub Pages. The domain comes from
  that setting, **not** from `docs/pages/CNAME` — the official builder strips that file from the
  output on purpose. `ci.yml`'s `Pages Site` job builds the same source on every PR, because
  `pages.yml` only runs on `main` and a broken template would otherwise reach the live site
  first. That job also asserts no page loads an asset over the network: this host serves the
  privacy policy, so a CDN font here would contradict the page it styles.
- `google-play.yml` — reusable workflow (`workflow_call`/manual dispatch) that uploads a tagged
  release's AAB to Google Play, package `app.larova`. Needs a `SERVICE_ACCOUNT_JSON` secret and an
  active Play Console listing. Checks out the *tag* rather than the default branch so the
  `versionCode` it reads matches the AAB, and aborts if `versionName` disagrees with the tag. It
  does **not** upload listing text or screenshots — `r0adkll/upload-google-play` handles only the
  AAB, mapping and release notes.

## Slash commands

`.claude/commands/` — repo-specific Claude Code commands:

- `/commit` — syncs, stages, drafts a conventional commit message, updates `CHANGELOG.md`'s
  `Unreleased` section when the change is user-facing, checks the staged diff against the
  invariants below, and offers to push.
- `/release` — the version bump and store-metadata step described above. Gated on
  `app/build.gradle.kts` existing, which it does not yet.

## Architecture

Kotlin Multiplatform with Compose Multiplatform, **Android as the only active
target for v1**. iOS is a later milestone, so platform-near code goes behind
`expect`/`actual` in `:core:platform` from the start rather than being sprinkled
through features. Module boundaries and the full stack table are in
`docs/technical-notes.md` §2–3:

```
:app  :feature:{home,card,help,transfer,settings}
:core:{domain,data,ui,platform}
```

Three layers, strictly one-directional: Compose UI reads state from a ViewModel →
ViewModel calls use cases → use cases call repositories. **The UI never reaches
into the data layer.** `:core:domain` has no platform dependencies.

Two structural decisions carry most of the weight:

- **`Card.payload` is serialized JSON of a sealed `CardPayload` interface.** New
  tile types need no database migration — only a new renderer. Unknown types
  arriving from a newer export are *skipped, not fatal*.
- **The export file is the whole backup and handover story.** A ZIP containing
  `manifest.json` (stays in the clear so the import preview works),
  `content.json` and `media/`. `schemaVersion` is the migration anchor and must
  be present from the very first export.

## Invariants

Break one of these and it cannot be repaired later, because users own the data.

1. **Store keys, never values.** `Card.colorToken` holds `"sage"`;
   `Card.icon` holds a symbol key. The theme resolves a key per appearance mode.
   A stored hex value makes every user tile unreadable in dark mode,
   retroactively, with no fix — the values were chosen by users. Same for icons:
   symbol keys, not bitmaps.

   The eight keys are `sand`, `clay`, `rose`, `lilac`, `sky`, `sage`, `moss`,
   `stone`, and they are frozen from the first release. A key is a value in
   `Card.colorToken` and in every export file, so renaming one later means
   migrating data that may only exist in a backup file nobody can reach. Picker
   labels come from `colour_<key>` in `strings.xml` and are localized; the key
   itself never reaches a user. Unknown keys resolve to the default rather than
   failing, which is what lets an export from a newer version still open.
2. **No hardcoded user-facing strings, from the first line of UI code.** All
   strings live in `strings.xml`; positional placeholders (`%1$s`) only; no
   concatenation in code; counts via `<plurals>` (Polish, Russian and Arabic
   have more than two forms). English is the source — never patch a translated
   file to fix wording, fix the English and re-translate.
3. **`start`/`end` in layouts, never `left`/`right`.** Arabic is in the launch
   set. Mirror directional icons; do not mirror clocks, phone numbers or media
   controls.
4. **Amber and alarm red are reserved.** Amber marks the active step and
   progress; alarm red is the help bar and nothing else. Neither is available as
   a tile colour — `sand` is the desaturated stand-in and the default for new
   tiles.
5. **Night is a function, not a third palette.** The dark palette pulled 55 %
   toward `#12101F` with warm ink; accents pulled 35 %. Do not add a third hex
   table that can drift.
6. **No internet permission, ever.** No analytics, no crash reporter with
   content access, no network dependency. This is on the store page and in the
   data safety form.
7. **Never interpret user content.** Larova stores and displays; it does not
   diagnose, calculate doses, score or trend. `docs/concept.md` §2.2 has the
   build / do-not-build table — that table is the scope check, and it exists to
   keep the app outside the EU MDR definition of a medical device.
8. **Media lives on disk, not in the database.** `filesDir/media/<uuid>.<ext>`
   with only the reference in the database. Images downscaled at import to a
   2048px long edge, JPEG q85.

## Android specifics worth knowing before reaching for an API

- **Calling** uses `Intent.ACTION_DIAL` with a `tel:` URI, so no `CALL_PHONE`
  permission and a mistaken tap is harmless. The app never dials by itself.
- **Backup destinations** come from `ActivityResultContracts.CreateDocument` /
  `OpenDocument`. Every installed cloud provider appears in the system dialog —
  there is no Drive SDK, no OAuth and no cloud integration on our side.
- **Media picking** uses `PickVisualMedia`, so no `READ_MEDIA_*` permission.
- **The app-shortcut picker** needs the `<queries>` manifest entry for
  `MAIN`/`LAUNCHER`. `QUERY_ALL_PACKAGES` is deliberately not used.
- Full permission list: `RECORD_AUDIO`, `CAMERA`, `POST_NOTIFICATIONS`
  (only if reminders ship). Nothing else.
- The manifest is hardened: `allowBackup="false"` plus restrictive
  `dataExtractionRules`, since the explicit export replaces cloud backup.

## Conventions

- Comments in the seed files explain *why* a rule exists, not what the code
  does. Match that.
- Accessibility is part of the work, not a later pass: 16sp body minimum, 22sp
  guide steps, 200 % font scale without clipping, 56dp touch targets, 4.5:1
  contrast, colour never the sole carrier of meaning, full TalkBack labelling.
- Project language is English throughout: code, comments, commit messages,
  documentation.
- Keep `CHANGELOG.md` current: an entry under `## [Unreleased]` for any
  user-facing change, in the same commit that makes it, not as a follow-up. CI,
  tooling and docs-only changes do not belong there.
- No unreviewed machine translation for anything a user reads — in-app strings
  or store text. Store text deserves *more* care, not less: a missing in-app
  string falls back to English, a missing store locale publishes blank.

## Licence

AGPL-3.0, single copyright holder. Two things would change that calculus and
should be raised before they happen: **the first accepted outside pull request**
(a DCO or CLA has to be in place first) and **any GPL/AGPL-licensed dependency**
(dual licensing only covers code we own). See `docs/technical-notes.md` §10.
