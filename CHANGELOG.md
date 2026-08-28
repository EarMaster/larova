# Changelog

All notable changes to Larova are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versioning
follows [Semantic Versioning](https://semver.org/). Release notes on GitHub Releases are extracted
automatically from the `## [x.y.z]` heading matching `versionName` in `app/build.gradle.kts` — see
`.github/workflows/release.yml`. Keep headings exact for that to keep working.

Entries describe what changed for someone *using* the app, in plain language. CI, tooling and
docs-only changes do not belong here.

## [Unreleased]

## [0.2.0] - 2026-08-28

### Added
- Larova fits a tablet. More tiles fit across the screen — three on a small one, four on a large
  one — and they stay the size they were rather than stretching. Anything you read instead of scan
  is held to a comfortable column in the middle of the screen rather than running to both edges,
  which is what a guide step read aloud across a room needs. Tiles are all one height now, so a
  screen of them reads as a grid.
- Larova speaks all fourteen launch languages: English, German, French, Italian, Spanish,
  Portuguese, Ukrainian, Polish, Russian, Turkish, Arabic, Hindi, Chinese (Simplified) and
  Japanese. English and German were written by hand; **the other twelve are machine translations
  that no native speaker has read yet**, which is worth knowing before relying on one — an
  unreviewed language is not a fallback, it is the whole app for whoever picks it.
- Backup & transfer says when a backup last ran and how much was in it, so "have I ever actually
  done this?" has an answer that does not involve going to look in a folder.

### Changed
- The activity log and settings are buttons on the start screen instead of items in a menu, and
  rearranging tiles is a button beside "Add tile" in parent view. Backup, which was the third
  thing behind that menu, has moved into settings where the rest of the parent-view work is.
- Backup & transfer is laid out as two things you can read and press in one go, rather than a
  heading, a paragraph and a button underneath each.
- The four appearance choices — follow the phone, light, dark, night — sit together in one panel
  now rather than as four loose rows, and each of them says what it looks like. The explanation of
  Night used to be a single line under all four, which read as a note about the lot of them and
  left the fair question of what was wrong with the other three.
- Settings and the activity log no longer carry the "Get help" bar. They are opened on purpose by
  a parent, and the red bar is for the moment something is wrong. Every screen a caregiver reads
  still has it.
- The log writes "opened a tile" lines small and quiet. They happen as a side effect of using the
  app, and at full size they buried the lines somebody sat down and typed.

### Fixed
- Tile names on the home-screen shortcuts are no longer cut off mid-word. Larova was shortening
  them itself at a fixed length; the phone's launcher shortens them properly, and only when it
  actually runs out of room.

## [0.1.0] - 2026-08-27

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
- An activity log, behind the menu in both views. Larova writes a line when a tile is opened, when
  something is ticked off and when a call is prepared, and whoever is with the child can add lines
  of their own — that is the part the parents come home to read. It is a list of what happened and
  nothing more: no totals, no trends, no interpretation. Lines older than 30 days are dropped, the
  log travels in a backup, and clearing it needs parent view.
- A first run is not an empty screen. Six starting points — Bedtime, Evening routine, Important
  contacts, Food and drink, A day with us, What helps when... — each put one real tile on the grid,
  filled in with example text to write over. They are yours from that moment: rewriting one is
  editing a tile, and switching the app language later does not touch what you have written.
- The three tiles opened most often turn up as shortcuts on the phone home screen, so what a
  caregiver reaches for every day is one tap away instead of two.
- Search sits at the top of the start screen and looks at tile names, not at what is inside them.
- A sound tile can be recorded on the spot — a parent reading the bedtime story onto the tile, so
  a caregiver who does not share the child's language can still play the right words. Larova asks
  for the microphone when the record button is tapped and not before.
- A tile can hold a video or a sound file: pick one from the phone, write a line above it, and it
  plays where it sits. Nothing starts by itself — a caregiver who opened a tile to read it should
  not have a video start talking at them. A file is copied into Larova rather than pointed at, so
  it travels in a backup, and a large one says so before it goes on a tile.
- A tile can open another app on the phone — the music player, the camera, the bus timetable.
  Pick it from a list of what is installed, and give the tile words a caregiver will recognise
  rather than the app store name. If the app is uninstalled later the tile says so instead of doing
  nothing when it is tapped.
- A folder tile holds other tiles: open it and the grid inside looks and works like the start
  screen. One level deep, on purpose — nobody searching under pressure should have to navigate.
  Deleting a folder says how many tiles go with it before it happens.
- Tiles can be rearranged from the menu, with buttons rather than dragging, on the start screen and
  inside a folder.
- Appearance can be set to follow the phone, or fixed to light, dark or **night** — warm and very
  dim, for reading aloud in a darkened room. The setting is remembered.
- The "Get help" bar is on every screen.
- A home-screen icon: the lit niche, including the monochrome version themed launchers use.
- Larova speaks German as well as English. On Android 13 and newer the phone can be told to show
  Larova in either one, whatever language the rest of the phone is set to — so a grandmother can
  read it in hers on a phone that stays in another.

The other twelve languages of the launch set are still to come.
