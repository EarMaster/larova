# Localization

Fourteen languages at launch. The app is translated; Larova never translates what a parent wrote.

---

## 1. The distinction that matters

**The app is translated.** Buttons, labels, settings, templates.

**Larova never translates user content.** What a parent writes stays exactly as written, in whatever language they wrote it. The app has no internet permission and never will, and every on-device translation library within reach downloads its models over the network — so there is no version of this where Larova does the translating.

**What it does instead is hand over.** A tile screen offers to pass the words on that tile to a translation app the person already has, the same way a call tile passes a number to the dialler and a website tile passes an address to the browser. The words go to an app they chose, under that app's permissions and that app's policy; nothing comes back, and nothing is stored. `plainTextOf` decides what "the words on a tile" means, and it deliberately excludes the numbers, addresses and package names — none of which are translatable, and all of which would break the tile if a translator's version of them were pasted back.

This is a change of position, made knowingly. The earlier wording here was *"there is no translation feature, and therefore no reason for content to ever leave the device"*, and the second half of that sentence stopped being true the moment the first half did. What has not changed is the part invariant 6 rests on: **Larova itself still sends nothing anywhere.** A handover is the person's own act, on their own tap, into an app of their own choosing.

See `docs/pages/privacy.md`, "Translation", for the same fact in the words a parent reads.

## 2. Why fourteen

The real need is not international reach. It is that **caregivers often do not share the parents' language**. A grandmother who reads no German still has to understand the back arrow and the help button on a phone whose system language is German.

That is why the per-app language setting matters more than the number of translations.

## 3. Language set

| Code | Language | Script | Direction | Notes |
| --- | --- | --- | --- | --- |
| `en` | English | Latin | LTR | **Default.** Source language for all strings |
| `de` | German | Latin | LTR | Strings run ~30 % longer than English |
| `fr` | French | Latin | LTR | |
| `it` | Italian | Latin | LTR | |
| `es` | Spanish | Latin | LTR | European Spanish; `es-419` deferred |
| `pt-PT` | Portuguese (Portugal) | Latin | LTR | |
| `uk` | Ukrainian | Cyrillic | LTR | |
| `pl` | Polish | Latin | LTR | Four plural forms |
| `ru` | Russian | Cyrillic | LTR | Four plural forms |
| `tr` | Turkish | Latin | LTR | Dotted/dotless i — never use locale-default `toUpperCase()` |
| `ar` | Arabic | Arabic | **RTL** | Six plural forms. Drives the whole RTL pass |
| `hi` | Hindi | Devanagari | LTR | |
| `zh-Hans` | Chinese (Simplified) | Han | LTR | |
| `ja` | Japanese | Japanese | LTR | No plural forms; no spaces for line breaking |

## 4. Implementation

**Per-app language.** Android 13+ exposes this through `LocaleManager` and `res/xml/locales_config.xml`. The caregiver sets the app to Turkish while the child's phone stays in German. Mirror it through `AppCompatDelegate.setApplicationLocales` for older versions. Put the picker prominently in settings, not in a submenu — it is a headline feature, not a preference.

**Resources.** All strings in `composeResources/values/strings.xml`, accessed via `stringResource`. No string concatenation in code. Placeholders always positional (`%1$s`), because word order changes. Plurals through `plurals` — Polish, Russian, Ukrainian and Arabic have more than two forms.

**The folder name is not the language tag.** Compose Multiplatform resources match on language and optional region, not on script, so the fourteen above live in `values`, `values-de`, `values-fr`, `values-it`, `values-es`, `values-pt-rPT`, `values-uk`, `values-pl`, `values-ru`, `values-tr`, `values-ar`, `values-hi`, `values-zh` and `values-ja`. Two are worth spelling out: `pt-PT` is `values-pt-rPT`, so a phone set to Brazilian Portuguese falls back to English rather than reading European wording; and `zh-Hans` is plain `values-zh`, which a `zh-Hans-CN` phone resolves to — a Traditional set later would be `values-zh-rTW`. These are again different from the Play locale codes in `fastlane/metadata/android/`.

**Templates belong in resources, not the database.** The shipped templates ("Bedtime", "Evening routine") are translatable strings copied into the database as user content when a template is used. From that moment they belong to the parents and no longer follow the app language.

**Right-to-left.** With Arabic in the set, this is not optional:

- `start`/`end` throughout in Compose, never `left`/`right`
- `android:supportsRtl="true"`
- Mirror directional icons: back arrow, chevrons. Do **not** mirror clocks, phone numbers or media controls
- The tile grid, the help bar and the guide step navigation all need checking specifically

Start early. Retrofitting RTL is expensive.

**Fonts.** Verify glyph coverage for Arabic, Devanagari, Han and Japanese. The bundled UI font almost certainly does not cover all four; rely on system font fallback and test on a clean device, not on a developer phone with extra fonts installed.

**Formats.** Dates, times and numbers through platform formatters and `kotlinx-datetime`. Never assembled by hand. They follow the selected app language, not the system.

**Layout.** German strings run ~30 % longer than English, Finnish longer still. No fixed widths. Tile titles need room for two lines. Test with pseudo-locales: `en_XA` for expansion and accents, `ar_XB` for direction.

## 5. Translation workflow

**Where the fourteen stand today.** `en` is the source and `de` is author-maintained. The other
twelve exist in full — every string, every plural form — as **model drafts that no native speaker
has read**. That was a deliberate call: a drafted language falls back to nothing, it *is* the app
in that language, so shipping one unread is a decision rather than an oversight, and it is recorded
here so nobody has to guess which files have been through a human.

Reviewing one is reading `core/ui/src/commonMain/composeResources/values-<lang>/strings.xml`
against `values/strings.xml` beside it.

Machine translation is an acceptable starting point for a hobby project, but not for these three,
which is where a review should start:

- **The help bar and contact sheet.** Read under stress. Must be unambiguous.
- **The two view names.** "Caregiver view" and "Parent view" carry the entire mental model.
- **The six templates.** They are the onboarding, and they need to sound like a person wrote them.

For those, find a native speaker. For the rest, machine translation followed by a native read-through is fine.

Two mechanical things a review should check that a fluent read does not automatically catch:

- **The plural forms are all there.** Polish, Russian and Ukrainian carry `one/few/many/other` and
  Arabic carries all six. A missing `few` only shows on 2, 3 and 4 and looks fine in every
  screenshot.
- **The placeholders survived.** `%1$d` and `%1$s` are positional so their *order* may change, but
  a lost one is a crash at the point somebody reads a step count.

Keep `en` as the single source. Never edit a translated file to fix wording — fix the English and re-translate, or the languages drift apart.

## 6. What a new language costs beyond `strings.xml`

Translating the app is the larger half of adding a language, but it is not all of it. Two other
things are keyed to the same list and go stale silently:

- **The store listing text**, one folder per Play locale under `fastlane/metadata/android/`. Store
  text has no English fallback: a locale with no file publishes blank in that language.
- **The store screenshots.** These are generated from the app rather than taken by hand, so they
  need the language twice over — the app's chrome follows the locale, and the tile titles in the
  pictures are *family content* and need their own fixture. `en-US` and `de-DE` have both today.
  The app now speaks all fourteen, so the blocker on the other twelve has moved: it is no longer
  the app, it is the twelve `StoreContent` fixtures nobody has written. Play falls back to the
  default locale's images for a locale that has none, so those twelve show the English pictures
  until then. See `AGENTS.md`, "Store assets", for the three lines and one fixture a new locale
  needs.

The order matters: `strings.xml` first, then the listing text, then the screenshots. A screenshot
taken before the app speaks the language captures the English fallback and looks convincing enough
that nobody notices.
