# AGENTS.md

Guidance for coding agents working in the Larova repository. This file is the
single source of truth; `CLAUDE.md` only points here.

## What this repository is

Larova is an **offline Android app** (Kotlin, Compose Multiplatform) that parents
set up on their child's phone: a grid of self-authored tiles — guides, notes,
checklists, tables, contacts, media — that a caregiver can read in two taps.
No account, no server, **no internet permission**.

**The project is currently documentation and seed files only.** There is no
Gradle build, no module structure and no commit history yet. `docs/` is the
design of record; `app/` holds three files written ahead of the build to lock in
decisions that are expensive to reverse.

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

CI (GitHub Actions) is specified to run build, unit tests, lint and Detekt on
every push. It is not set up yet either.

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

1. **Store keys, never values.** `Card.colorToken` holds `"salbei"`;
   `Card.icon` holds a symbol key. The theme resolves a key per appearance mode.
   A stored hex value makes every user tile unreadable in dark mode,
   retroactively, with no fix — the values were chosen by users. Same for icons:
   symbol keys, not bitmaps.
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

- Colour token keys are German (`salbei`, `flieder`) because they are internal
  identifiers that never reach a user; picker labels are localized strings. If
  this is to change, it must change **before** any user content exists.
- Comments in the seed files explain *why* a rule exists, not what the code
  does. Match that.
- Accessibility is part of the work, not a later pass: 16sp body minimum, 22sp
  guide steps, 200 % font scale without clipping, 56dp touch targets, 4.5:1
  contrast, colour never the sole carrier of meaning, full TalkBack labelling.
- The HTML prototypes predate the switch to English and still carry German
  annotations. Sample content is fine to leave; annotations get translated when
  those screens are next revised.

## Licence

AGPL-3.0, single copyright holder. Two things would change that calculus and
should be raised before they happen: **the first accepted outside pull request**
(a DCO or CLA has to be in place first) and **any GPL/AGPL-licensed dependency**
(dual licensing only covers code we own). See `docs/technical-notes.md` §10.
