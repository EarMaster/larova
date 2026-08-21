# Localization

Fourteen languages at launch. The app is translated; user content is not.

---

## 1. The distinction that matters

**The app is translated.** Buttons, labels, settings, templates.

**User content is not.** What a parent writes stays exactly as written, in whatever language they wrote it. There is no translation feature, and therefore no reason for content to ever leave the device.

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

**Resources.** All strings in `composeResources/values/strings.xml`, accessed via `stringResource`. No string concatenation in code. Placeholders always positional (`%1$s`), because word order changes. Plurals through `plurals` — Polish, Russian and Arabic have more than two forms.

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

Machine translation is an acceptable starting point for a hobby project, but not for these three:

- **The help bar and contact sheet.** Read under stress. Must be unambiguous.
- **The two view names.** "Caregiver view" and "Parent view" carry the entire mental model.
- **The six templates.** They are the onboarding, and they need to sound like a person wrote them.

For those, find a native speaker. For the rest, machine translation followed by a native read-through is fine.

Keep `en` as the single source. Never edit a translated file to fix wording — fix the English and re-translate, or the languages drift apart.
