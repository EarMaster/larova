# Changelog

All notable changes to Larova are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versioning
follows [Semantic Versioning](https://semver.org/). Release notes on GitHub Releases are extracted
automatically from the `## [x.y.z]` heading matching `versionName` in `app/build.gradle.kts` — see
`.github/workflows/release.yml`. Keep headings exact for that to keep working.

Entries describe what changed for someone *using* the app, in plain language. CI, tooling and
docs-only changes do not belong here.

## [Unreleased]

### Added
- Larova installs and opens on a start screen of your own tiles.
- Tiles can hold a guide, a note, a checklist, a table, a number to call, or a website. Guides
  show one step at a time at reading size, and the screen stays awake while one is open. Checklist
  items can be ticked off without unlocking anything. A table takes your own headings, up to four
  of them, and each cell is read out under the heading it belongs to.
- A call tile opens the phone app with the number ready. Larova never dials by itself, and a call
  tile can be marked to appear behind "Get help" as well — a short list of large rows, capped so it
  stays readable when it is needed.
- Tiles can be made, changed and deleted: pick what goes on it, give it a title, choose one of the
  eight colours and a symbol. Deleting asks first, because there is no undo and no bin.
- Two views. Caregiver view is what the app opens in: read, tick, call, open. Editing, rearranging
  and backup live in parent view, which unlocks with a PIN or a fingerprint and falls back to
  caregiver view after five minutes without a touch. The PIN keeps the tiles from being changed; it
  does not hide them, and the app says so before you choose one.
- Backup and restore. "Back up" writes everything — tiles, pictures, recordings — into one file,
  wherever the phone can save: device storage, Drive, Nextcloud, a USB stick. "Restore" shows what
  is in a file before it touches anything, then either replaces what is here or adds to it. A file
  that arrived incomplete is refused rather than half-applied, and one written by a newer Larova
  says so instead of losing what it cannot read.
- A step of a guide can carry a picture, picked from the photos on the phone. It is copied into
  Larova rather than pointed at, so tidying up the gallery later cannot empty a guide, and it is
  made smaller on the way in — a backup full of camera-sized photographs is one nobody can send
  anywhere. A picture that is taken off a step is deleted once no step wants it.
- Search sits at the top of the start screen and looks at tile names, not at what is inside them.
- Tiles can be rearranged from the menu, with buttons rather than dragging.
- Appearance can be set to follow the phone, or fixed to light, dark or **night** — warm and very
  dim, for reading aloud in a darkened room. The setting is remembered.
- The "Get help" bar is on every screen.
- A home-screen icon: the lit niche, including the monochrome version themed launchers use.
- Larova speaks German as well as English. On Android 13 and newer the phone can be told to show
  Larova in either one, whatever language the rest of the phone is set to — so a grandmother can
  read it in hers on a phone that stays in another.

The other twelve languages of the launch set are still to come.
