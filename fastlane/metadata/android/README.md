# Play Store listing metadata

Google Play *store listing* text, one directory per locale, in the layout `fastlane supply` and
Triple-T's gradle-play-publisher both read natively. Keeping it here means listing copy is
reviewed in PRs like any other translated string instead of being retyped into the Play Console.

| File | Play limit | Status |
|---|---|---|
| `title.txt` | 30 characters | present |
| `short_description.txt` | 80 characters | present |
| `full_description.txt` | 4000 characters | present — **`de-DE` needs a native read-through** |
| `changelogs/{versionCode}.txt` | 500 characters | written per release by `/release` |

## Store locales are not app locales

Two directories here — `en-US` and `de-DE` — against fourteen in-app languages
(`docs/localization.md`). That gap is deliberate, not an omission: the plan ships the Play listing
in English and German at M3, and the remaining twelve translations land at M4 while the listing
stays as it is.

The asymmetry that justifies it: a missing in-app string falls back to English, while a store
locale with no text publishes **blank** in that language. An untranslated listing is worse than no
listing, so a locale appears here only once its text actually exists.

Adding one means three files (`title`, `short_description`, `full_description`), a release-note
file for the current versionCode, and adding the locale to `LOCALES` in
`tools/check_store_metadata.sh` — the check only looks at locales named there.

## Release notes are named after the versionCode

`changelogs/7.txt` is the What's New text for **versionCode 7** — not version 0.7.0. That is how
fastlane and the Play API key release notes, and it is the easy thing to get wrong: a file named
after the versionName, or one left at the previous versionCode, is silently never picked up and
the release ships with empty release notes.

Every store locale is required for every release. `google-play.yml` enforces this and fails the
deploy rather than shipping a gap.

## Keep the listing inside the product boundary

Larova stores and displays what a parent writes; it never interprets it. That boundary is what
keeps the app outside the EU MDR definition of a medical device (`docs/concept.md` §2.2), and the
store listing is where it is most likely to be crossed by accident — a phrase like "keep track of
your child's health" claims an intended purpose the app does not have.

Concretely, per `docs/concept.md` §2.3: category Tools or Lifestyle (not Medical, not Health &
Fitness), no medical screenshots, target audience 18+, and a data safety form answered as no data
collected and no data shared. The privacy claim is worth stating plainly in the text, because it
is unusually strong and entirely true: no account, no cloud, no internet permission.

## Screenshots

Not written yet. They belong at `en-US/images/phoneScreenshots/` and are due in M3 alongside the
rest of the store assets. `check_store_metadata.sh` already validates them — format, alpha channel,
dimensions, aspect ratio and count — so a screenshot Play would refuse fails here first. The most
common refusal is the aspect ratio: a screenshot from a modern 20:9 phone is 2.2:1, and Play caps
the long side at twice the short one.

Play falls back to the default language's images for a locale that has none, so English-only is
fine.

## What CI does with these files

`google-play.yml` reads `changelogs/{versionCode}.txt` for every locale, validates it, and copies
it into the flat `whatsnew-<locale>` naming `r0adkll/upload-google-play` expects.

That action does **not** upload listing text — title, short description and full description are
still copied into the Play Console by hand. These files are the canonical source; the Console is
the copy. Closing that gap means adding a `fastlane supply` step or moving to
gradle-play-publisher, either of which would read this tree as-is.
