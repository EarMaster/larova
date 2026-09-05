# Changelog

All notable changes to Larova are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versioning
follows [Semantic Versioning](https://semver.org/). Release notes on GitHub Releases are extracted
automatically from the `## [x.y.z]` heading matching `versionName` in `app/build.gradle.kts` — see
`.github/workflows/release.yml`. Keep headings exact for that to keep working.

Entries describe what changed for someone *using* the app, in plain language. CI, tooling and
docs-only changes do not belong here.

## [Unreleased]

## [0.6.0] - 2026-09-05

### Added
- Tiles can be written in more than one language. In parent view a tile now has an "Other
  languages" section: pick a language and Larova copies the tile’s words into a form for you to
  change — the title, the steps, the items, one box each. The original is never touched, and the
  Translate button is there to get a starting point from a translation app. Larova translates
  nothing itself and never reads what you paste.
- A tile that has more than one language shows them as buttons at the top; tapping one switches
  every tile on the phone to that language, so a caregiver sets it once. A tile that has not been
  translated is shown exactly as written — never hidden, never greyed out. If you change a tile
  after translating it, the translation says so rather than pretending to be current.
- A "Tile language" setting to put that choice back, or change it without opening a tile.
- Tiles can now be translated. A tile opened by somebody who does not read the language it was
  written in has a globe in the top bar: tapping it hands the words on that tile to a translation
  app already on the phone. Larova itself translates nothing and sends nothing anywhere — it
  passes the words to an app you chose, the same way tapping a contact passes a number to the
  phone app. Phone numbers, web addresses and app names are left out, because a translated one
  would stop working. If there is no translation app on the phone, the globe is not shown.
- A Language row in the settings, for the person holding the phone. It opens Android’s own
  language screen for Larova, where you can set the app to a different language from the rest
  of the phone — so a caregiver can read the buttons in their own language without changing
  anything else. Needs Android 13 or newer; on older phones the row is not shown.

## [0.5.2] - 2026-09-04

### Fixed
- The version built from source — the APK on the GitHub release page — now says where to help
  pay for the development, and tapping the full-version card opens that page in the browser.
  It named no address at all, and the card it should have been on did nothing when tapped.
  Nothing changes in the version from Google Play: there the same card asks Play again, and
  asking for a donation beside something you can buy would be asking twice.

## [0.5.1] - 2026-09-04

### Fixed
- Tapping the full version in the settings now answers you. While Larova asks Google Play the
  card says so, and if the store has no purchase for this phone you get told why — a different
  Google account, or a store that could not be reached — with the option to buy the full version
  right there. It used to check silently and leave the card looking exactly as it had before.

## [0.5.0] - 2026-09-04

### Changed
- The full-version section of the settings now looks like the backup and contribution sections
  around it: one card, with the icon, the status and the explanation inside it, and tapping the
  card is what asks Google Play again. It used to be loose text with a small link under it, which
  read like part of a different screen.
- Choosing a tile type that needs the full version now shows you the tile first. The fields are
  laid out as usual, greyed but readable, with one "Locked — tap to unlock" button above them, so
  you can see what a Video or Sound tile actually asks for before deciding whether it is worth
  paying for. The offer with the price arrives when you tap that button. It used to appear straight
  away, on top of the very fields it was describing.
- The text about the full version is shorter and says the part that matters: one payment, no
  subscription, everything else stays free, and anything you make can always be shared without a
  second purchase.

### Fixed
- Restoring a backup that contains a kind of tile this version does not know now brings back
  everything else and says how many it had to leave out — and that they are still in your file, so
  updating Larova and restoring again gets them. Before, one unfamiliar tile made the whole file
  refuse to open, and it said "this is not a Larova backup" about a perfectly good one.
- The messages when a restore cannot go ahead now say what to do about it. A file that could not be
  opened at all — one still sitting in cloud storage, most often — no longer claims not to be a
  Larova backup, and the others suggest a next step instead of stopping at the bad news.
- Replacing everything when you restore is harder to do by accident: the dialog now says that it
  removes what is on the phone first and cannot be undone, the two choices no longer sit in the
  order that favoured the destructive one, and there is a Cancel button rather than only tapping
  outside.

## [0.4.2] - 2026-09-03

### Added
- You can support the development from the settings, once parent view is on. It is a contribution
  rather than a purchase: nothing is unlocked by it, it can be given more than once, and the app
  keeps count of how many times you have.
- The settings now say whether the full version is unlocked, with a way to ask Google Play again.
  That helps if you bought it on another phone, or if this phone was offline when Larova last
  started. A version built from source says so instead of showing a lock.
- The app version is shown at the bottom of the settings, so it is at hand when something needs
  reporting.

### Fixed
- When more than one price is on offer for the full version — during a sale, for instance — Larova
  now shows and charges the lower one. It could previously have shown one price and charged
  another.

## [0.4.1] - 2026-09-03

### Changed
- Picking a tile type that belongs to the full version now shows the tile as it would be, with the
  offer laid over it, instead of a message in front of a screen you had not seen. You can look at
  what you would be getting before deciding, and choose a different type without dismissing
  anything first.
- The offer now says plainly that the full version belongs to the Google account that buys it.
  Google Play cannot share an in-app purchase between family members the way it shares a paid app,
  so it is better said before paying than discovered afterwards.

## [0.4.0] - 2026-09-01

### Added
- App, video and sound tiles are now part of a paid full version, bought once through Google Play.
  Everything else stays free: guides, notes, checklists, tables, call tiles, folders, websites,
  backups, the parent-view lock, night mode and all fourteen languages.
- Tiles somebody else made keep working whether or not you have bought anything. A backup restored
  from another phone arrives complete, and every tile in it opens and plays. What the full version
  buys is *making* app, video and sound tiles — not opening them.
- The app asks Google Play once per launch what you already own, so a new phone, a reinstall or a
  payment that finished after you closed the app all restore the full version on their own. There is
  no button for it and nothing to find.
- The version built from source, including the APK on the GitHub release page, has no paid tier at
  all and never asks for anything.

## [0.3.2] - 2026-08-30

### Fixed
- Searching for a symbol could close the app. Three of them were in the list twice, and the moment
  a search brought both copies of one onto the screen together, the picker gave up. Each is offered
  once now.

## [0.3.1] - 2026-08-30

### Added
- 291 symbols to choose from, and choosing one is now its own screen rather than a block of icons
  wedged into the tile editor. The sixty-eight everyday ones are offered first as suggestions, and
  the search covers all of them. Every symbol shows its name, so a symbol you liked is one you can
  find again.

### Changed
- "Website or app" was one entry in the tile chooser that could make either kind of tile, with no
  way to tell which you were getting. They are two now — "Website", which opens a link in the
  browser, and "App", which opens something on the phone.
- A website tile and an app tile can both carry a line saying what they are for, the way a video
  tile can. "Line 142 from the corner" is what makes a bookmark usable by somebody who did not
  save it.

### Fixed
- The red "Get help" bar no longer appears on the screens only a parent sees: backup and transfer,
  the tile editor, rearranging, unlocking parent view and choosing a PIN. It stays where a
  caregiver reads — the start screen, every tile, and the help sheet itself.

## [0.3.0] - 2026-08-29

### Added
- Sixty-eight symbols to choose from instead of ten — beds, buses, a bath, a plaster, a rabbit —
  grouped into everyday things, food and drink, care, home, out and about, play, people and notes,
  with a search box for finding one by name. Tiles you already made are untouched: the ten original
  symbols are still there under the same names, redrawn to match the rest.
- Screenshots on larova.app open large when you press one, with the arrows to step through them.

### Changed
- A call tile holds as many people as you need instead of one. Each has their own name, what they are to
  the child, and their number, and each can be marked to appear behind "Get help" on its own — so
  one tile can hold the doctor, the neighbour and the grandmother while only two of them are behind
  the red bar. Opening the tile shows them as the same rows the help sheet uses, because it is the
  same act: press a person, the phone app opens with their number in it. The tile on the start
  screen says how many numbers are on it.
- A tile made before this still opens and still shows its one number, and a tile made now still
  opens in an older Larova — showing the first of its numbers rather than refusing the file. A
  backup written today can be restored on a phone that has not been updated yet.

## [0.2.1] - 2026-08-29

### Changed
- The four appearance choices — follow the phone, light, dark, night — sit together in one panel
  rather than as four loose rows, and each of them says what it looks like. Only Night was
  explained before, which read as a note about all four and left the fair question of what was
  wrong with the other three.

### Fixed
- Tile names on the home-screen shortcuts are no longer cut off mid-word. Larova was shortening
  them itself at a fixed length; the phone's launcher shortens them properly, and only when it
  actually runs out of room.

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
- The appearance setting explains "Night" underneath the word Night, instead of as a line under
  all four options where it looked like a note about the lot of them.
- Settings and the activity log no longer carry the "Get help" bar. They are opened on purpose by
  a parent, and the red bar is for the moment something is wrong. Every screen a caregiver reads
  still has it.
- The log writes "opened a tile" lines small and quiet. They happen as a side effect of using the
  app, and at full size they buried the lines somebody sat down and typed.

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
