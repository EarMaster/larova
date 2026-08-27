# Play Store listing metadata

Google Play *store listing* text, one directory per locale, in the layout `fastlane supply` and
Triple-T's gradle-play-publisher both read natively. Keeping it here means listing copy is
reviewed in PRs like any other translated string instead of being retyped into the Play Console.

| File | Play limit | Status |
|---|---|---|
| `title.txt` | 30 characters | present, all locales |
| `short_description.txt` | 80 characters | present, all locales |
| `full_description.txt` | 4000 characters | present, all locales — **twelve need a native read-through, see below** |
| `changelogs/{versionCode}.txt` | 500 characters | written per release by `/release` |

## All fourteen languages, and why

One directory per language the app itself speaks (`docs/localization.md` §3). The reason is the
same one that motivates the per-app language picker: a caregiver who does not share the parents'
language should be able to read the listing that offered them the app in the language they will
actually use it in. A Turkish-speaking grandmother finding an English-only listing has already hit
the problem the app exists to solve.

This is a standing cost, not a one-off. Every release needs a release note in all fourteen, and
`google-play.yml` fails the deploy rather than shipping a gap — store text has no English
fallback, so a locale with no file publishes **blank** in that language.

**Play locale codes are not the app's language tags.** These directory names are what the Play API
accepts, and four of them differ from what `locales_config.xml` uses: Chinese Simplified is
`zh-CN` here and `zh-Hans` there, Hindi is `hi-IN`, Japanese `ja-JP`, and Ukrainian and Arabic
carry no region at all (`uk`, `ar`). A code Play does not recognise is rejected at upload, not by
the local check.

Adding or changing a locale means three files (`title`, `short_description`, `full_description`),
a release-note file for the current versionCode, and the locale being listed in `LOCALES` in
`tools/check_store_metadata.sh` — the check only looks at locales named there.

## Translation status

`en-US` is the source. `de-DE` is maintained by the author. **The other twelve are drafts written
by a language model and have not been read by a native speaker.** Per `docs/localization.md` §5
that is not the standard this project holds itself to for anything a user reads, and store text
deserves more care than in-app strings rather than less: a missing in-app string falls back to
English, while store text is the first and sometimes only thing a caregiver reads.

Highest priority for a review pass, because they are the load-bearing sentences: the short
description (it appears in search results), the "two views" paragraph — "caregiver view" and
"parent view" carry the whole mental model — and the closing "what Larova is not", which is a
regulatory boundary and not just copy.

The RTL languages need a second kind of check: Arabic listing text renders in the Play Store's own
layout, so verify the punctuation and the Latin fragments (`Larova`, `112`, the licence name) read
correctly in context rather than only in a text editor.

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

Two workflows, on two clocks, because a release and a listing are different things going wrong.

**`google-play.yml`**, per release: reads `changelogs/{versionCode}.txt` for every locale, validates
it, and copies it into the flat `whatsnew-<locale>` naming `r0adkll/upload-google-play` expects,
alongside the AAB and its mapping file.

**`play-listing.yml`**, whenever these files change on `main`: runs `fastlane supply` over this tree
and pushes the title, both descriptions and any images to the Console. That closes the gap this
section used to describe — the tree is the source and the Console is downstream of it, rather than
somebody retyping fourteen languages.

**It cannot bootstrap a store entry.** `fastlane supply` handles listing text and release notes in
one pass, and that pass starts by finding a track and a release to attach the notes to — before it
has looked at whether changelogs were skipped. So the listing upload needs a release on the internal
track to exist already, and it needs the versionCode of one (`play-listing.yml` reads it from
`app/build.gradle.kts`). The first AAB still goes up by hand, per `docs/release-setup.md` §3.7; this
publishes the listing of an app that has shipped somewhere, not the listing of one that has not.

Three more things are worth knowing before relying on it:

- **It uploads no binary and no release notes.** Notes are keyed by versionCode and belong beside
  the AAB that contains them; attaching whatever is in the tree to whatever is currently live is
  how a build ships with somebody else's notes.
- **The edit is committed but not sent for review.** A new app whose first release has not been
  reviewed cannot have changes submitted through the API at all — Play refuses the edit and names
  the parameter that avoids it. So the text lands in the Console and waits for somebody to press
  *Send for review*, which is the right shape for fourteen languages of copy either way.
- **Images already on Play are left alone.** `sync_image_upload` is off, so a screenshot uploaded
  through the Console survives a text fix here. When screenshots land in this tree (M3) that
  decision is worth revisiting: with it on, the tree becomes authoritative for graphics too.

A pull request that touches these files validates them through Play's own API without changing
anything, which is the only check that catches a locale code Play does not accept.
