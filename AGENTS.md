# AGENTS.md

Guidance for coding agents working in the Larova repository. This file is the
single source of truth; `CLAUDE.md` only points here.

## What this repository is

Larova is an **offline Android app** (Kotlin, Compose Multiplatform) that parents
set up on their child's phone: a grid of self-authored tiles — guides, notes,
checklists, tables, contacts, media — that a caregiver can read in two taps.
No account, no server, **no internet permission**.

**M0 has landed: the project builds.** Ten Gradle modules, the design system, the
data model, and a navigable shell that switches between light, dark and night.
Everything user-facing beyond that is still ahead — see `docs/implementation-plan.md`.
`docs/` remains the design of record and outranks the code where the two disagree.

**Fixed identifiers.** Application ID `app.larova`; domain `larova.app` (the
reversed domain, so the ID is verifiably ours). Neither can change after the
first Play release. The domain's first job is the privacy policy URL Play
requires on the listing, `https://larova.app/privacy` — the one store asset an
offline app cannot supply for itself. It does not weaken the no-internet claim:
the app never resolves the domain, and has no permission with which to.
See `docs/technical-notes.md` §7.

**The app and the store listing both exist in all fourteen languages**, and both
carry the same caveat. `en` / `en-US` is the source, German is
author-maintained, and **the other twelve — in-app strings and listing text
alike — are model drafts that no native speaker has read.** That is a decision
the maintainer took knowingly, not an oversight, and it must keep being said out
loud rather than quietly shipped: a drafted language does not fall back to
anything, it *is* the app for whoever picked it. `docs/localization.md` §5 names
the parts a review starts with. Play's locale codes are not the app's language
tags (`zh-CN` not `zh-Hans`, `hi-IN`, `ja-JP`, bare `uk` and `ar`) — read the
directory names, don't derive them; the app's own resource folders are
`values-<lang>` with `values-pt-rPT` and `values-zh` the two that are not a bare
two-letter code.

## Current state of the tree

| Path | Contents |
| --- | --- |
| `docs/concept.md` | Product, users, regulatory boundary, feature scope |
| `docs/technical-notes.md` | Stack, module layout, data model, export format, Android specifics |
| `docs/implementation-plan.md` | Milestones M0–M5 and their exit criteria |
| `docs/localization.md` | The fourteen launch languages and the rules around them |
| `docs/design/design-system.md` | Colour tokens, icon, typography, layout |
| `docs/design/prototypes/*.html` | Clickable references: screen flow, icon sheet, colour grid |
| `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml` | The ten modules; Detekt and the `test` alias applied to all of them; every dependency version in one place |
| `gradle.properties` | `larova.compileSdk` / `targetSdk` / `minSdk` / `jvmTarget`, read by every module build file so they cannot drift |
| `config/detekt/detekt.yml` | Deltas on Detekt's defaults, each with the reason it exists |
| `app/` | The only Android-specific module: manifest, launcher icon, navigation graph, Koin composition root |
| `app/src/testDebug/` | Screenshot tests and their golden PNGs — the app module's only tests. In the *debug* unit test source set, not `test`, because the activity they compose into is contributed to the debug manifest by `ui-test-manifest`. See "Screenshot tests" |
| `core/ui/src/commonMain/kotlin/.../theme/AppColors.kt` | The colour token table and mode resolution |
| `core/ui/src/commonMain/composeResources/values/strings.xml` | English base strings — the single source. Compose resources, **not** an Android resource directory: the screens live in `commonMain` and cannot see an `R` class |
| `core/{domain,data,platform}/` | Models and the payload codec; Room schema, DAOs and repositories; `expect`/`actual` paths |
| `core/data/schemas/` | Generated Room schema JSON. **Committed** — the only record of what a released database version looked like |
| `feature/{home,card,help,transfer,settings}/` | One screen each, all still placeholders except the appearance switch |
| `app/src/main/res/values/strings.xml` | The launcher label alone. The manifest can only reference an Android resource; everything else lives in `:core:ui` |
| `app/src/main/res/xml/locales_config.xml` | Per-app language list |
| `brand/*.svg` | Adaptive icon layers and the store icon |
| `core/ui/icons/` | The 291 tile symbols as SVG, filed in a folder per picker shelf — `lucide/` vendored under ISC, `larova/` drawn here. Turned into Compose `ImageVector`s by the `generateTileSymbols` task; the file name is the frozen symbol key and the folder is its shelf. See that folder's `README.md` |
| `CHANGELOG.md` | [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) history. **Update `## [Unreleased]` in the same commit as any user-facing change.** `release.yml` extracts GitHub Release notes from the `## [x.y.z]` heading matching `versionName`, so headings must stay exact |
| `fastlane/metadata/android/` | Play Store listing text, one folder per store locale in `fastlane supply` layout, plus the listing screenshots under `en-US/images/` generated by `StoreAssetTest` (see "Store assets"). Written by `/release`, validated by `tools/check_store_metadata.sh`, consumed by `google-play.yml` (release notes) and `play-listing.yml` (the listing itself). See that folder's `README.md` |
| `fastlane/Fastfile`, `fastlane/Appfile`, `Gemfile` | The one Ruby in the project: `fastlane supply` pushing the listing to Play. Nobody runs it locally — `play-listing.yml` resolves the gems on the runner, which is why there is no committed `Gemfile.lock` |
| `tools/check_store_metadata.sh` | Validates listing text against Play's per-locale character limits (title 30, short 80, full 4000, release notes 500) and listing images (24-bit PNG, no alpha, 320–3840 px a side, long side at most twice the short one, at least two per set). Pass a versionCode to also require release notes in every store locale |
| `.claude/commands/` | Repo slash commands: `/commit` and `/release` — see "Slash commands" |
| `docs/pages/` | The GitHub Pages site (Jekyll), deployed by `pages.yml` — landing page (with the generated screenshots in `assets/screenshots/`) and privacy policy, served at `larova.app`. Built on the MIT-licensed Hydra template, vendored; see its `_config.yml` for what was stripped out and why |
| `docs/release-setup.md` | One-time signing and publishing setup: upload key, the four GitHub secrets, Play Console prerequisites, branch protection |

## Commands

```bash
./gradlew assembleDebug                        # build
./gradlew test                                 # unit tests, all modules
./gradlew :core:domain:test                    # unit tests, one module
./gradlew lint detekt                          # Android Lint + Detekt
./gradlew installDebug                         # to a device or emulator
./gradlew connectedAndroidTest                 # instrumented tests (none yet; M1)

./gradlew :app:verifyRoborazziDebug --tests '*ScreenshotTest'   # compare against the goldens
./gradlew :app:recordRoborazziDebug --tests '*ScreenshotTest'   # accept what the UI looks like now
./gradlew :app:recordRoborazziDebug --tests '*StoreAssetTest'   # rebuild the Play/website images
```

Two things about that list are not obvious.

**`test` in a multiplatform module is an alias this repository registers.** The real task is
`testAndroidHostTest`, aggregated by `allTests` and `testAndroid`; such a module has no `test` task
of its own. The alias is in the root build file because `ci.yml` runs `./gradlew test` — without it
that command reports success while running nothing.

**Gradle needs JDK 17 or newer, and the JDK on `PATH` here is not it.** Use the JBR that ships with
Android Studio: `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"`. `local.properties` must
carry `sdk.dir` — it is gitignored, and Lint fails the build if the drive letter's colon is
unescaped (`sdk.dir=D\:/Users/...`).

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
  verification until it reaches a PR. Both start with a `detect` job gated on a root `./gradlew`;
  that gate is satisfied since M0, so those jobs now actually run. `ci.yml`'s screenshot job has a
  second gate on a `*ScreenshotTest*.kt` existing, which is satisfied too now that the goldens are
  in — and it reports a mismatch as a warning rather than a failure. Read the comment in the file
  for what that costs, and "Screenshot tests" below for the rest.
- `screenshots.yml` — re-records the goldens *and* the store/website images on the runner and
  commits the result. Never automatic: it runs from the Actions tab, or on a push to `develop` whose
  commit message contains `[record-screenshots]`. See "Screenshot tests".
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
  handles the AAB, the mapping and the release notes only.
- `play-listing.yml` — the listing text and images, through `fastlane supply`, on pushes to `main`
  that touch `fastlane/**` plus manual dispatch (with a validate-only option). Uploads no binary
  and no release notes, and leaves the edit **committed but not sent for review** — a new app whose
  first release has not been reviewed cannot submit changes through the API, and unattended copy in
  fourteen languages is not something to publish without a human. A pull request touching those
  paths validates the same payload against Play without changing anything.

## Slash commands

`.claude/commands/` — repo-specific Claude Code commands:

- `/commit` — syncs, stages, drafts a conventional commit message, updates `CHANGELOG.md`'s
  `Unreleased` section when the change is user-facing, checks the staged diff against the
  invariants below, and offers to push.
- `/release` — the version bump and store-metadata step described above. `app/build.gradle.kts`
  now exists at `versionName 0.1.0` / `versionCode 1`, so the command works — and so does
  `release.yml`, which builds and publishes as soon as `main` moves. Both read those two fields
  with `grep`/`sed` and take the **first** match in the file, so keep them plain assignments and do
  not mention either identifier in a comment above them.

## Architecture

Kotlin Multiplatform with Compose Multiplatform, **Android as the only active
target for v1**. iOS is a later milestone, so platform-near code goes behind
`expect`/`actual` in `:core:platform` from the start rather than being sprinkled
through features. Module boundaries and the full stack table are in
`docs/technical-notes.md` §2–3.

Three things about how that is built are worth knowing before editing a build file. AGP 9 has
**built-in Kotlin support**, and applying `org.jetbrains.kotlin.android` beside it fails outright.
Library modules use **`com.android.kotlin.multiplatform.library`**, configured inside
`kotlin { android { … } }`, rather than `com.android.library` plus `androidTarget()` — the old
pairing needs an opt-in under AGP 9 and disappears in AGP 10; the price is single-variant libraries,
host tests behind `withHostTestBuilder {}`, and **Android resources switched off by default** —
which is not a resource question but a packaging one. `:core:ui` has to set
`androidResources.enable = true` or the Compose plugin has nowhere to put the compiled strings, and
the app builds green with no text in it at all. See the comment in `core/ui/build.gradle.kts`. And **`:app` is a plain Android application
module**, because there is no multiplatform equivalent of `com.android.application`; iOS will bring
its own entry point beside it rather than through it.

Compose artifacts come from the plugin's `compose.*` accessors instead of the version catalog:
`material3` has its own version line and is stable at 1.9.0 while runtime, ui and foundation are at
1.11.x, so a single catalog version does not resolve. The accessors are deprecated as of 1.11;
revisit when material3 catches up.

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
- Full permission list: `RECORD_AUDIO` for recording a voice onto a tile, plus
  `USE_BIOMETRIC`/`USE_FINGERPRINT` from `androidx.biometric` for the parent-view
  unlock. `POST_NOTIFICATIONS` only if reminders ship, and `CAMERA` only if a
  capture screen is ever built inside the app — the photo picker means one is not
  needed. **Nothing else, and that includes what a library brings with it:**
  `media3-exoplayer` contributes `ACCESS_NETWORK_STATE` and `WAKE_LOCK` through
  manifest merge, and both are removed with `tools:node="remove"`. "View network
  connections" on an app whose whole promise is having no internet permission reads
  as a contradiction, whatever the permission actually grants. Check
  `aapt2 dump badging` after adding any dependency.
- The manifest is hardened: `allowBackup="false"` plus restrictive
  `dataExtractionRules`, since the explicit export replaces cloud backup.

## Screenshot tests

The UI is rendered to PNGs and compared on every pull request. The tests are in
`app/src/testDebug/kotlin/app/larova/screenshot/`, the goldens in
`app/src/testDebug/screenshots/`, one PNG per state, committed so a review sees the picture next to
the diff that changed it.

They run **on the JVM**: Robolectric renders the real Compose UI with native graphics, Roborazzi
writes and compares the files. No emulator, no device.

Three groups, and the split matters:

- **`home/`, `card/`** — the start screen and every tile type, each in **all four appearances**:
  light, dark, night and 200 % font scale, **plus a tablet**. This is the matrix
  `docs/implementation-plan.md` asks M3 for, and it is wide on purpose (see the first bullet below).
  The tablet capture is not an appearance: it is 1280×800dp landscape, where the grid gains two
  columns and everything that is read is capped and centred, and a renderer can get that wrong in
  a way no phone picture shows.
- **`tokens/`** — all eight colour tokens and all ten symbol keys in one picture per mode, plus the
  help bar. The two tables invariant 1 freezes, in the three places they resolve differently.
- **`screens/`** — the screens that are not about appearance: search, an empty grid, parent view,
  the help sheet, settings, backup and restore, the import dialog, the log, unlock, PIN setup and
  the editor. Light and tablet only, deliberately: the mode does not change them and the width
  does.

Things worth knowing before touching any of it:

- **The three-mode matrix exists for invariant 1.** A tile stores `"sage"` and `"moon"`, never a hex
  value; the theme resolves the pair against the active mode. A token that resolves wrong in night
  is a screen of unreadable tiles, and it is completely invisible in the light-mode picture everyone
  looks at. Anything that carries a token gets all four appearances; anything that is theme colours
  all the way down (settings, the log) gets one, because the other three would be pictures of
  `MaterialTheme` rather than of Larova.
- **No fakes, no DI, no database.** Every Larova screen takes its state as a parameter and hands its
  events back out, so a picture is made from `Fixtures.kt` and nothing else. Adding a screen to the
  set is a fixture and three lines. This is a property of the architecture worth not losing: a
  screen that reached for a repository would drag Koin, Room and DataStore into every golden.
- **One capture per test method.** The compose rule refuses a second `setContent` on the same
  activity, so a screen photographed in four appearances is four subclasses of one abstract test —
  `LightCardScreenshotTest`, `DarkCardScreenshotTest` and so on — never a loop inside one method.
- **Compose Multiplatform strings need `isIncludeAndroidResources`, and the assets rather than the
  resources.** The plugin compiles `strings.xml` into `assets/composeResources/`, and the reader
  that loads them finishes *after* the first frame — so `show()` idles before capturing. Without
  either, a golden is a picture of the layout with every label empty.
- **Nothing is written during `./gradlew test`.** `captureRoboImage` is inert unless Roborazzi's own
  tasks set its system property, so an ordinary test run only checks that every screen still
  composes — worth having on its own, given `:app` had no tests at all before.
- **A failing verify uploads what it saw.** `_actual` and `_compare` images land in
  `app/build/outputs/roborazzi/` and `ci.yml`'s `Screenshots` job uploads them. That directory is
  flat, so **golden file names must stay unique across subfolders**: two `grid_light.png` in
  different directories would overwrite each other's diff.
- **A mismatch is a warning, not a failure**, and that is a deliberate weakening — the reasoning is
  written out in `ci.yml` next to the `continue-on-error`. The cost is not hedged: nothing else in
  CI looks at pixels, so a real visual regression merges green unless somebody reads the warning and
  the diff artifact.
- **A tablet golden is a `@Config` and two lines.** `TABLET_QUALIFIERS` in `ScreenshotTest.kt` is
  the frame; a subclass carrying it and `variant = TABLET_VARIANT` is the whole of adding one.
- **Recording locally is fine.** Robolectric brings its own fonts and its own Skia, so the host
  contributes far less than one would expect. What does matter is the **SDK level and the JDK major
  version** — see the `sdk=35` note in `app/src/testDebug/resources/robolectric.properties`, pinned
  to what Temurin 17 can run because that is what every workflow here uses. Keep those in step
  rather than the operating system.
- **The tests are debug-only** and live in the `testDebug` source set for it: the activity
  `createAndroidComposeRule` launches is contributed to the *debug* merged manifest by
  `ui-test-manifest`. Their dependencies are declared `testDebugImplementation` to match, which
  leaves `testReleaseUnitTest` empty rather than broken.
- **`composeAndroidx` in the version catalog has to be kept in step by hand.** The Compose test
  artifacts have no multiplatform alias, so they are named against the AndroidX build that
  `org.jetbrains.compose` resolves to on Android. Check it with
  `./gradlew :app:dependencies --configuration debugRuntimeClasspath` after any Compose bump, or the
  test classpath quietly carries two Composes.

### Recording is a decision

`screenshots.yml` re-records the goldens *and* the store images on the runner and commits the
result. It runs either from the Actions tab (`workflow_dispatch`, available once the workflow has
reached `main` at a release) or on a push to `develop` whose **commit message contains
`[record-screenshots]`** — put that marker in the commit that changes the UI. Its own commit carries
no marker, so it cannot loop. A local `recordRoborazziDebug` is equally valid; the files come out
the same.

## Store assets

`StoreAssetTest` generates the Play listing screenshots and the website's, from the same harness as
the goldens, into `fastlane/metadata/android/<locale>/images/` — five `phoneScreenshots` at
1233×2460 and five `tenInchScreenshots` at 2560×1600, per locale. Two sets because Play has two
slots and Larova has two layouts; stretching the phone set into the tablet slot would advertise a
two-column grid the app does not draw above 840dp. The **English set alone** is copied into `docs/pages/assets/screenshots/`,
because the website is served from `docs/pages` only, cannot link into the fastlane tree, and is
written in English. `README.md` links straight at the English fastlane copies.

**They are products, not baselines**, and that distinction runs through everything here:

- They are **excluded from verify** by the `--tests` filter. A UI change should fail one job, not
  two, and they are converted after capture so they would never match byte for byte anyway.
- **Play's rules decide the frame, not the goldens'.** A screenshot must be a 24-bit PNG with no
  alpha, every side between 320 and 3840 px, and the long side at most twice the short one. The
  goldens' 1078×2399 frame is 2.23:1 and would be rejected outright, which is the whole reason these
  are generated separately rather than copied across. The store frame is 1.995:1 — as tall as Play
  allows and no taller, because every dp of height is a dp of a guide that fits without scrolling.
- **The alpha channel is stripped in Gradle**, in `finishStoreAssets`, because `java.awt` and
  `javax.imageio` are not on an Android unit test's classpath. `recordRoborazziDebug` is finalised
  by it, so one command still does the whole job. In a Kotlin DSL build script the imports must be
  at the top of the file: a bare `java.awt.Color` parses as a property on the Java plugin extension,
  which is what `java` means there.
- **`tools/check_store_metadata.sh` validates the images**, not only the listing text — format,
  alpha, dimensions, ratio, size and count. `google-play.yml` already calls it, so a bad image fails
  a deploy rather than an upload.
- **A locale needs two halves, and the qualifier is the easy one.** The app's chrome follows the
  `@Config` locale; the tile titles, guide steps and names do not, because they are *family
  content*. They come from a `StoreContent` in `StoreFixtures.kt`, one per locale. A set generated
  with only the first half is a listing in one language showing an app apparently set up in
  another, which is worse than no localised set at all — and a screenshot is the one listing asset
  that cannot be fixed later by editing a text file, because the words are baked into a picture.
- **English and German today, and the limit has moved.** It used to be the app: `values/` and
  `values-de/` were the only string sets, so a French screenshot would have been French tile names
  inside an English UI. The app now ships all fourteen, so what is missing is the *other* half —
  twelve `StoreContent` fixtures, the family content in the pictures, which nobody has written.
  Play falls back to the default locale's images wherever a locale has none, so those twelve show
  the English set until then.
- **Every locale the app gains should gain a set.** That is three lines and a fixture: a
  `StoreContent` in `StoreFixtures.kt` written by somebody who speaks the language, and a
  `StoreAssetTest` subclass carrying the Play locale directory and a `@Config` qualifier with the
  Android locale in front of the size (`fr-rFR-w411dp-…`). Nothing else needs touching —
  `finishStoreAssets` walks every locale directory it finds, and `check_store_metadata.sh` already
  validates every locale's images.

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

One set of files here is somebody else's: `core/ui/icons/lucide/`, vendored from
Lucide 1.37.0 under the **ISC** licence, with their notice beside them. ISC is
permissive and compatible, so it changes nothing about the calculus above — but
the notice has to travel with the files, which is why it is committed rather than
referenced. They are vendored rather than depended on so that a frozen symbol key
never resolves through somebody else's naming; Lucide dropping its `smile` and
`frown` between releases is exactly the failure that would otherwise have taken a
symbol off a family's tile.
