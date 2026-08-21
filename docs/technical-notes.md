# Technical notes

Architecture, data model, export format and the Android specifics that shape them.

---

## 1. Platform choice

| Approach | Assessment |
| --- | --- |
| **Kotlin Multiplatform + Compose Multiplatform** | **Chosen.** Android stays fully native, data and UI layers are shared, iOS can be added later without a rebuild. Platform-near parts (intents, file pickers, video) go through `expect`/`actual` |
| Android-only (Kotlin, Compose) | Fastest route to v1. Reasonable if iOS is uncertain — converting later is possible but costs the UI layer |
| Flutter | Solid, but intents, package visibility and system dialogs run through third-party plugins — exactly the areas that are central here |
| React Native / Capacitor | Weakest fit: local media handling and filesystem work are the core of this app |

**Practical stance:** build with Compose Multiplatform, but target version 1 at Android only. Draw the module boundaries properly from the start so the iOS step is platform adapters and UI polish rather than a rewrite.

## 2. Stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.x |
| UI | Compose Multiplatform, Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + `StateFlow`, unidirectional data flow |
| DI | Koin |
| Database | Room (KMP) or SQLDelight |
| Serialization | kotlinx.serialization |
| Images | Coil 3 |
| Video/audio | Media3 ExoPlayer (Android), AVPlayer (iOS) behind a shared interface |
| Crypto | Google Tink (Android), CryptoKit (iOS) |
| Archives | `java.util.zip` (Android), `expect`/`actual` for iOS |
| Tests | kotlin.test, Turbine, Compose UI tests, Roborazzi for screenshots |

## 3. Modules

```
:app                Entry point, navigation graph, theme
:feature:home       Tile grid, search, reordering
:feature:card       Display and editing per tile type
:feature:help       Help bar and contacts
:feature:transfer   Backup, restore, share
:feature:settings   View mode, PIN, log, appearance, language
:core:domain        Models, use cases, no platform dependencies
:core:data          Repositories, DAOs, media store
:core:ui            Design system, shared components
:core:platform      expect/actual: intents, file pickers, crypto, zip
```

Three layers: Compose UI reads state from a ViewModel, the ViewModel calls use cases, use cases talk to repositories. The UI never reaches into the data layer.

## 4. Data model

```kotlin
data class Board(
    val id: Uuid,
    val parentId: Uuid?,          // null = start screen
    val title: String,
    val sortIndex: Int,
    val updatedAt: Instant,
)

data class Card(
    val id: Uuid,
    val boardId: Uuid,
    val title: String,
    val subtitle: String?,
    val icon: String,             // symbol key, never a bitmap
    val colorToken: String,       // token key, never a hex value — see below
    val sortIndex: Int,
    val visibleToCaregiver: Boolean,
    val type: CardType,
    val payload: String,          // JSON
    val locale: String?,          // reserved for per-card second language (v2)
    val updatedAt: Instant,
)

@Serializable
sealed interface CardPayload {
    @Serializable @SerialName("guide")
    data class Guide(val steps: List<Step>) : CardPayload

    @Serializable @SerialName("checklist")
    data class Checklist(val items: List<CheckItem>, val resetDaily: Boolean) : CardPayload

    @Serializable @SerialName("table")
    data class Table(val columns: List<String>, val rows: List<List<String>>) : CardPayload

    @Serializable @SerialName("phone")
    data class Phone(val displayName: String, val number: String, val relation: String?) : CardPayload

    @Serializable @SerialName("appLink")
    data class AppLink(val packageName: String, val label: String, val deepLink: String?) : CardPayload

    // note, video, audio, web, folder analogous
}

@Serializable
data class Step(val text: String, val mediaId: Uuid?, val audioId: Uuid?)

data class MediaAsset(
    val id: Uuid,
    val relativePath: String,     // media/<uuid>.<ext>
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
)

data class LogEntry(
    val id: Uuid,
    val at: Instant,
    val kind: LogKind,            // CARD_OPENED, CHECK_TOGGLED, CALL_PREPARED, MANUAL_NOTE
    val cardId: Uuid?,
    val note: String?,
)
```

**The trick** is `payload` as serialized JSON of a sealed interface. New tile types arrive without a database migration; only the renderer is extended. Unknown types from a newer export are skipped rather than blowing up the import.

**The rule that cannot be fixed later:** `colorToken` and `icon` store keys, not values. The theme resolves a key into one of three surface colours depending on the mode. Storing the hex value instead makes every user-created tile unreadable in dark mode — retroactively, with no way to repair it, because the values were chosen by users.

## 5. Media

Large files do not belong in the database. They live in app-private storage under `filesDir/media/<uuid>.<ext>`; the database holds only the reference. This means no access from other apps, automatic deletion on uninstall, and no storage permission.

Imported images are downscaled to a maximum long edge of 2048px and stored as JPEG at quality 85. Videos are not transcoded, but a size threshold triggers a warning. A cleanup job removes assets with no remaining reference.

## 6. Export format

```
larova-2026-08-21.larova            (ZIP container)
├── manifest.json                   schema version, timestamp, checksum, encryption
├── content.json                    boards, cards, payloads, log
└── media/
    ├── 3f2a….jpg
    └── 91cd….m4a
```

```json
{
  "schemaVersion": 1,
  "appVersion": "1.0.0",
  "exportedAt": "2026-08-21T18:12:00Z",
  "label": "Larova for Jonas",
  "counts": { "boards": 2, "cards": 14, "media": 9 },
  "encryption": "none",
  "contentSha256": "…"
}
```

Optional encryption: AES-256-GCM, key derived from the password with Argon2id. `manifest.json` stays in the clear so the preview works; `content.json` and `media/` are encrypted.

`schemaVersion` is the migration anchor. Import checks it first and declines newer versions politely.

## 7. Android specifics

**Choosing a destination without a Drive SDK.** `ActivityResultContracts.CreateDocument("application/octet-stream")` opens the system dialog. Every installed cloud provider appears there as a destination, Google Drive included. No OAuth, no API approval, no Google verification. `OpenDocument` is the counterpart for import.

**Calling.** `Intent.ACTION_DIAL` with a `tel:` URI. Hands off to the phone app rather than dialling — needs no `CALL_PHONE` permission and is harmless if triggered by mistake. Direct dialling only as a deliberately enabled option.

**Opening other apps.** From Android 11, other apps are invisible by default. The picker for creating a shortcut needs this in the manifest:

```xml
<queries>
  <intent>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
  </intent>
</queries>
```

The `QUERY_ALL_PACKAGES` permission is deliberately **not** used — it requires justification on Play and is unnecessary here.

**Picking media** through `PickVisualMedia` (photo picker). No `READ_MEDIA_*` permission required.

**Full permission list.** `RECORD_AUDIO` (voice notes, runtime), `CAMERA` (direct capture, runtime), `POST_NOTIFICATIONS` (only if reminders ship). **No internet permission.** That is a claim worth putting on the store page.

**Manifest hardening.** `android:allowBackup="false"` plus restrictive `dataExtractionRules` — potentially sensitive content should not ride along in automatic cloud backups. The explicit user-triggered export replaces it. `FLAG_SECURE` optionally for the guide screen.

**Quick access.** App shortcuts for the three most-used tiles; optionally a widget carrying the help bar. For the lock screen, point users at Android's built-in emergency information rather than building our own.

## 8. Security

- App lock via `BiometricPrompt` with PIN fallback, for parent view only
- Database encryption with SQLCipher as an option in settings; off by default, since app-private storage is already encrypted on current Android versions
- PIN stored as an Argon2id hash in the Keystore, never in the clear
- No analytics and no crash reporter with content access. If crash reporting becomes necessary: self-hosted, no user content, opt-in

## 9. Quality and release

- GitHub Actions: build, unit tests, lint, Detekt on every push
- Screenshot tests for every tile type across light, dark, night and 200 % font scale
- Instrumented test for the critical path: export → uninstall → reinstall → import → everything back
- Play Console: internal testing → closed testing with actual grandparents → production
- Play App Signing, R8 with keep rules for kotlinx.serialization

## 10. Licensing note

AGPL-3.0 is an unusual fit for a fully offline app — the network clause in section 13 never triggers, so in practice it behaves exactly like GPL-3.0. Keeping AGPL costs nothing and future-proofs any later server component.

### App stores and the GPL family

Apple's App Store terms impose usage rules on downloaded software, such as limits on the number of devices. The FSF holds that these are exactly the further restrictions the GPL family forbids a distributor from adding; GNU Go and VLC were both pulled from the store over this. Google Play has no comparable clause, so Android is unaffected.

**This does not block an iOS release here, because the project has a single copyright holder.** A licence binds licensees, not the author. The same code can go out under AGPL in the repository and under Apple's standard EULA in the store — ordinary dual licensing, with nobody's rights to infringe. The conflict only arises for someone distributing *other people's* AGPL code, which is what happened in both cases above.

A section 7 app store exception was considered and dropped. It would benefit forks rather than this project, it weakens the copyleft the AGPL was chosen for, and `WITH app-store-exception` is not a registered SPDX identifier, so licence scanners trip on it. It can be added on the day iOS becomes real.

**Two things that would change this:**

1. **The first accepted pull request.** From then on someone else is a co-author and can block relicensing. If outside contributions ever become likely, put a DCO or CLA in place beforehand, not afterwards.
2. **A GPL- or AGPL-licensed dependency.** Dual licensing only covers code you own. Rare on Android — almost everything is Apache 2.0 — but it is the thing to watch when adding libraries.
