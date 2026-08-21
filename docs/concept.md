# Concept

What Larova is, who it serves, and the decisions that shape it.

---

## 1. In one paragraph

An offline app that parents set up on their child's phone. It holds a start screen of tiles they arrange themselves; each tile carries a guide, a list, a table, a video, a phone number or a shortcut. Caregivers — grandparents, daycare, babysitters, coaches — open the app, find what they need in two taps, and can reach the parents immediately. Everything is stored locally. There is no account, no server, and no internet permission. Content is backed up and shared through a single export file.

**The promise:** everything someone needs to know about my child, in a place anyone can understand.

---

## 2. Positioning

### 2.1 A container, not a health app

Larova knows about "tile with steps", "tile with a list", "tile with a number". It does not know about diagnoses, medications, measurements or doses. What goes into a tile is written by the parents; the app never interprets it.

This is an architectural decision, not a marketing one. It is also what keeps the product legally simple.

### 2.2 Regulatory boundary

In the EU, whether software is a medical device is determined by its **intended purpose** (MDR Art. 2(1)), not by its app store category. Software that only stores, archives and displays medical data without interpreting it generally falls outside the definition per MDCG 2019-11.

The practical consequence:

| Build | Do not build |
| --- | --- |
| Free-text guides written by parents | Pre-written medical content or treatment pathways |
| Checklists with user-defined items | Symptom questionnaires that produce recommendations |
| Tables with user-defined columns | Dose calculators, weight- or substance-based logic |
| "Show tile X at 18:00" reminders | Medication reminders backed by a drug database |
| A call button for a stored number | Automatic alerting of emergency services |
| A log entry: "guide opened at 14:32" | Evaluation, trend display, interpretation of that log |

As long as the right column stays empty, Larova is a notebook with templates.

### 2.3 Store listing

- **Category:** Tools or Lifestyle. Not Medical, not Health & Fitness.
- **Screenshots and description:** bedtime, evening routine, handover to grandparents, holiday care, daily schedule, important contacts. No medical screenshots.
- **Google Play target audience:** 18+. The app is configured and read by adults, even though it lives on a child's device. Declaring a child audience triggers the entire Families policy, which is neither necessary nor desirable here.
- **Data safety form:** no data collected, no data shared. This can be answered honestly and is a genuine selling point.
- **Health apps declaration:** not applicable, as no health functionality is advertised.
- **Package name:** `app.larova`, from the registered domain `larova.app`. Fixed from the first release — see `technical-notes.md` §7.
- **Privacy policy:** `https://larova.app/privacy`. Play requires a working URL on the listing, and it is the one store asset the offline architecture cannot supply for itself.
- **Listing languages:** all fourteen the app speaks, not just the launch market's two. A caregiver who needs the per-app language picker is exactly the person who will read the listing in that language first — see `localization.md` §2 and `fastlane/metadata/android/README.md`.

### 2.4 Privacy as a feature

Anything a parent writes about their child's health is a special category of personal data under GDPR Art. 9. The local-only architecture answers this cleanly: no processing by an operator, no processor agreement, no transfer. The privacy policy fits on half a page.

This belongs prominently in the store listing: **no account, no cloud, no internet permission.**

---

## 3. Users

**Setting up (parents).** Creates tiles, maintains content, sets a PIN, makes backups. Uses the app irregularly but intensively when they do.

**Reading (caregivers).** Opens an unfamiliar app under time pressure, sometimes with poor eyesight, sometimes without smartphone habits. Must manage without explanation and must not be able to break anything.

**The child.** In later years uses some tiles directly — read-aloud, step-by-step, "I've done this".

### Scenarios

1. **A night at the grandparents'.** Grandma opens "Bedtime", follows five steps with pictures, and plays the song the mother recorded.
2. **First day at daycare.** The teacher skims "What helps when he cries" and "Food and drink".
3. **Something goes wrong.** A bar at the bottom of every screen reads "Get help"; behind it are two or three stored numbers, one tap to dial.
4. **A new caregiver.** The parents export a package and send it by messenger. The other person imports it into their own installation.
5. **Looking back.** In the evening the parents see which tiles were opened and which checklist items were ticked.

---

## 4. Functionality

### 4.1 Information architecture

```
Start screen (tile grid, freely arranged)
├── Tile → Guide          (steps with text, image, audio)
├── Tile → Video
├── Tile → Audio
├── Tile → Note
├── Tile → Checklist
├── Tile → Table
├── Tile → Call
├── Tile → Website
├── Tile → App shortcut
└── Tile → Folder → further tiles

Always visible:  Search · Get help
Via menu:        Switch view · Log · Backup & transfer · Settings
```

Deliberately flat: two levels at most. Someone searching under pressure should not have to navigate.

### 4.2 Two views

**Caregiver view** is the default state: read, tick, call, open. No editing, no deleting, no settings.

**Parent view** unlocks with a PIN or biometrics. Only then do the plus button, edit handles, reordering, deletion and export appear. After five minutes of inactivity the app falls back automatically.

This is the single most consequential decision in the product. It makes the interface radically simple for the primary audience and protects the content without anyone having to understand a permission model.

### 4.3 Get help

A bar pinned to the bottom of every screen, in the signal colour. Tapping it opens a sheet with the stored contacts, each large, with a photo and a relation.

Explicitly **no** automatic emergency call and **no** automatic location sharing. The call is prepared in the phone app; the caregiver triggers it. This keeps the app out of emergency-services regulation and makes misuse harmless.

Optional and disabled by default: a prepared message ("I'm with [child] and need a hand") that opens in the messenger but is not sent by the app.

### 4.4 Log

A local event log that records without being asked: tile opened, checklist item ticked, call prepared. Caregivers can add their own entries.

For the parents this is the actual documentation feature. It stays a plain event list with no evaluation, scoring or trends. Kept 30 days by default, adjustable, clearable, and included in exports.

### 4.5 Backup and transfer

One menu item doing two things.

**Back up** produces a single file. The destination is chosen through the Android system dialog, which lists device storage, Google Drive, Nextcloud and every other installed provider. No cloud integration is needed on our side.

**Restore** picks a file, shows a preview ("12 tiles, 4 videos, created on …"), then replaces or merges.

Optional password protection, recommended by default whenever the file is shared with someone else.

### 4.6 Onboarding

No empty screen on first launch, but a choice of templates that put something meaningful on screen immediately: Bedtime, Evening routine, Important contacts, Food and drink, A day with us, What helps when…

Each template is a pre-structured skeleton with example text to overwrite. It doubles as the answer to "what is this actually for?".

### 4.7 Appearance

Three settings: system, light/dark, and **night**. Night exists because the leading use case is reading a guide aloud in a darkened bedroom — warm amber on near-black, no bright surfaces, reduced display brightness, reachable directly from the guide screen.

See [design/design-system.md](design/design-system.md) for the colour tokens and the rule that makes this work: tiles store a colour **key**, never a hex value.

### 4.8 Accessibility

Not a follow-up topic. Part of the primary audience is over 65.

- Minimum 16sp, guide steps at 22sp, honours system font scale up to 200 %
- Touch targets at least 56dp; tiles considerably larger
- Contrast at least 4.5:1 throughout; colour never the sole carrier of meaning
- Full TalkBack labelling with a sensible focus order
- Read-aloud for guides and notes via system text-to-speech
- Screen stays awake inside guides

---
