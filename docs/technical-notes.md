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

**Identifiers.** The application ID is `app.larova`, and the domain is `larova.app` — the same two words either way round, which is the point: the ID is the reversed domain, so it is verifiably ours and cannot collide. Both are permanent. An application ID cannot be changed after the first Play release without shipping a different app that shares no update path, no signing identity and no installed base with this one.

The domain earns its keep before any website does. Play requires a **privacy policy URL** on the listing, and it must resolve for a real reviewer — `https://larova.app/privacy` is that page, and it is the one store asset with no offline substitute. Two more uses follow from it and are worth reserving now rather than retrofitting: `larova.app` as the `android:host` for any App Link, and a documented MIME/extension registration for the `.larova` export file so a package arriving by messenger opens in the app rather than in a text viewer.

Nothing about the domain weakens the no-internet claim. The app itself never resolves it: the privacy policy is a link the Play Store shows, opened in the browser by the person reading the listing, and the app has no internet permission with which to fetch anything.

**The paid unlock, and how it keeps the no-internet claim true.** App, video and sound tiles are sold once through Google Play (`app.larova.unlock`). That decision runs straight into invariant 6, so the reasoning is written down rather than left to be rediscovered.

Play Billing does no networking in this process. `BillingClient` binds over IPC to `com.android.vending`, which does the network under its own permissions, and the library's manifest declares exactly one permission: `com.android.vending.BILLING` — `protectionLevel` normal, declared by the Play Store, in no permission group, so it grants nothing and appears in no permission list on the phone. Play's listing shows an "In-app purchases" badge instead, which is the honest place for it to show.

Its POM is the problem. `com.android.billingclient:billing:9.1.0` depends on `com.google.android.datatransport` — `transport-backend-cct` declares `INTERNET` and `ACCESS_NETWORK_STATE` and contributes a `TransportBackendDiscovery` service; `transport-runtime` adds `ACCESS_NETWORK_STATE`, a `JobInfoSchedulerService` and an `AlarmManagerSchedulerBroadcastReceiver`. It is Firelog: Google's own analytics pipeline. `play-services-base`, `play-services-basement` and `play-services-tasks` declare no permissions at all, and `play-services-location` is pulled in for nothing this app does.

So the group is excluded outright in `core/billing/build.gradle.kts`, rather than merely denied a permission — invariant 6 says "no network dependency", and shipping the uploader with its permission stripped would be true on a technicality only.

That exclusion is safe by the library's own design, and this was checked in the bytecode the same way media3's `NetworkTypeObserver` was. `com.android.billingclient.api.zzdt` is the logging shim. Its constructor wraps its entire body in a catch-all — exception table `from 4 to 42 target 43 type any` — and on failure sets a boolean field. Its `zza` method then returns early with the string literal `"Skipping logging since initialization failed."` A `NoClassDefFoundError` is a `Throwable`, so it is caught; the library is built to run without this backend, and the only thing lost is Google's own analytics. R8 needs `-dontwarn com.google.android.datatransport.**` for the dangling references, which is in `app/proguard-rules.pro` with the same note.

Three layers hold the line, because the first is a dependency declaration a version bump can undo without anyone noticing: the exclusion, a `tools:node="remove"` on `INTERNET` in the manifest that is a deliberate no-op today, and an assertion in `ci.yml` on the built APK's permission list. **Re-check the bytecode after every billing bump.** If a future version moves that construction outside the try, the symptom is a crash the moment `BillingClient` is built; the answer is then to keep the dependency and strip the permissions and the three components in the manifest instead, and to record here that the telemetry code ships even though it cannot run.

**Verification without a server.** There is no server to validate a purchase token against and there never will be, so the check is the offline one: `Purchase.originalJson` and `Purchase.signature`, verified with `SHA1withRSA` against the licensing public key from Play Console, embedded at build time. The key is public and anybody who can rebuild the app can replace or delete the check — under the AGPL, legally. The point is not to be unbreakable; it is to make a purchase mean something with no network, and to make a `true` written into a preferences file by hand not count. What is stored is the signed receipt, never a boolean, and it is re-checked on every read.

The entitlement is **cache-positive and never downgrades**. A failed store query is the ordinary case on a child's phone, and a successful but *empty* answer is deliberately not treated as a revocation either: it is what a refund looks like, but also what a phone signed into a different Google account looks like, and this app is installed on one person's phone and set up by another. A family losing a paid unlock because a parent signed out is a support problem; a refunded family keeping it is a rounding error.

**Two artifacts, one source.** `-Plarova.paidTier=false` produces a build with no paid tier, which is what `release.yml` attaches to the GitHub Release. Play Billing cannot transact for an app Play did not install, so a sideload build could not sell anything anyway, and a crippled one would buy nothing while costing goodwill with the people most likely to contribute. The `LICENSE` note in `README.md` carries the AGPL section 7 additional permission for linking the proprietary library, scoped narrowly by keeping billing in one module.

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
