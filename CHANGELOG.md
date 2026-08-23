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
- Tiles can hold a guide, a note, a checklist, a number to call, or a website. Guides show one
  step at a time at reading size, and the screen stays awake while one is open. Checklist items
  can be ticked off without unlocking anything.
- A call tile opens the phone app with the number ready. Larova never dials by itself.
- Tiles can be made, changed and deleted: pick what goes on it, give it a title, choose one of the
  eight colours and a symbol. Deleting asks first, because there is no undo and no bin.
- Search sits at the top of the start screen and looks at tile names, not at what is inside them.
- Tiles can be rearranged from the menu, with buttons rather than dragging.
- Appearance can be set to follow the phone, or fixed to light, dark or **night** — warm and very
  dim, for reading aloud in a darkened room. The setting is remembered.
- The "Get help" bar is on every screen.
- A home-screen icon: the lit niche, including the monochrome version themed launchers use.

The tiles on the start screen are still examples rather than your own, and nothing can be edited
yet. Both arrive with the tile editor.
