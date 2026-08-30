# Implementation plan

Sequenced so that the riskiest thing is proven earliest and every milestone ends with something installable.

Estimates assume one person working part-time on a hobby schedule. Treat them as ordering, not as dates.

---

## Guiding order

Two things decide whether this project works, and both are cheap to get wrong and expensive to fix later:

1. **The colour token indirection.** Tiles must store a key, not a hex value. Wrong once, wrong forever, because users pick the colours.
2. **Export and import round-tripping.** If content cannot survive a reinstall, the app has no right to ask anyone to fill it with anything.

Both are pulled into M1 rather than left for a polish phase.

---

## M0 — Foundation

*Roughly 2–3 weeks*

Nothing user-visible. The point is that later milestones cost what they should.

- Gradle project, module boundaries as in `technical-notes.md`, Compose Multiplatform configured with Android as the only active target
- Design system: colour tokens from `AppColors.kt`, typography scale, spacing, shape, three appearance modes wired to a theme switch
- Room schema for `Board`, `Card`, `MediaAsset`, `LogEntry`; `CardPayload` as a sealed interface with kotlinx.serialization
- Navigation graph with placeholder screens
- CI: build, unit tests, lint, Detekt
- `locales_config.xml` and `values/strings.xml` in place; **no hardcoded strings from day one** — retrofitting this is miserable

**Done when:** an empty app installs, switches between light, dark and night, and a colour token resolves correctly in all three.

---

## M1 — MVP

*Roughly 6–8 weeks*

The smallest thing a real family could actually use.

- Start screen: tile grid, create, edit, reorder, delete, search
- Tile types: **guide, note, checklist, call, website**
- Caregiver view and parent view, PIN with biometric unlock, 5-minute auto-return
- Help bar on the caregiver's screens, contact sheet, `ACTION_DIAL` handoff
- Backup and restore: ZIP container, manifest with `schemaVersion`, system file picker for destination and source, merge-or-replace on import
- Images in guides via the photo picker, downscaling, app-private media store
- English and German

**Done when:** the round-trip test passes — fill the app, export, uninstall, reinstall, import, everything is back including images.

**Deliberately out of scope:** video, audio, tables, app shortcuts, folders, the log, encryption. Every one of them is additive and none blocks the shape of the product.

---

## M2 — Completing the toolbox

*Roughly 4–5 weeks*

- Tile types: **video, audio, table, app shortcut, folder**
- Voice recording, playback via Media3
- App picker using the `<queries>` manifest entry
- Event log with manual entries, 30-day retention, clearing, inclusion in exports
- Six onboarding templates, authored carefully — they carry the whole first-run experience
- App shortcuts for the three most-used tiles

**Done when:** every tile type in `concept.md` exists and survives an export round trip.

---

## M3 — Ready for strangers

*Roughly 4 weeks*

The milestone that decides whether anyone keeps using it.

- Password-protected export: AES-256-GCM, Argon2id key derivation
- Accessibility pass: TalkBack labelling and focus order, 200 % font scale, contrast audit, touch target audit, read-aloud via TTS
- Screenshot tests across all tile types × three modes × 200 % scale
- Manifest hardening, R8 keep rules, Play App Signing
- Store assets: icon from `brand/`, screenshots, listing text in English and German, data safety form, 18+ audience declaration
- **Closed testing with actual grandparents.** Not colleagues. This is the only test that tells you whether the caregiver view is genuinely self-explanatory

**Done when:** someone over 65 who has never seen the app finds a bedtime guide and makes a call without being told how.

---

## M4 — Languages

*Roughly 3–4 weeks*

- Remaining 12 languages per `localization.md`
- RTL pass for Arabic: `start`/`end` throughout, mirrored directional icons, layout audit
- Font fallback verification for Arabic, Hindi, Chinese, Japanese
- Per-app language picker prominent in settings
- Pseudo-locale testing (`en_XA`, `ar_XB`)

**Done when:** every screen holds together in Arabic and in pseudo-locale expansion.

Splitting this out from M3 is deliberate: an English-and-German release can ship and be learned from while translations are still arriving.

---

## M5 — iOS

*Roughly 5–7 weeks, unscheduled*

- Platform adapters: file picker, dialling, media playback, zip, crypto
- SwiftUI polish where Compose Multiplatform defaults feel un-native
- App Store submission. Sole copyright holder, so the AGPL in the repository and Apple's EULA in the store coexist — see `technical-notes.md`

Do not start this before the Android version has real users. Everything learned there changes what gets built here.

---

## Rough total

Around four to five months of part-time work to a Play Store release covering M0–M3, plus M4 shortly after.

---

## Risks

| Risk | Mitigation |
| --- | --- |
| Colour or icon values stored instead of keys | Enforced in M0, before any content exists |
| Export format changes after users have data | `schemaVersion` from the first export; unknown card types skipped, not fatal |
| Caregiver view still too complex | Tested with real grandparents in M3, not at launch |
| Scope creep into health features | The "do not build" table in `concept.md` is the check |
| Media inflating backup size beyond usability | Downscale images at import, warn on large videos, show package size before export |
| Motivation fading on a hobby schedule | Every milestone ends installable; M1 alone is already useful to its author |
