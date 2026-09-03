package app.larova.core.domain

/**
 * A `content.json` exactly as the shipped app writes one, captured from the tree at `v0.4.2`
 * before the wire format changed.
 *
 * A verbatim literal rather than something produced by calling the writer. A fixture generated at
 * test time stops being evidence the moment the writer changes — it would quietly start testing the
 * new format against itself. This string cannot drift, and `"type": "GUIDE"` is readable in the
 * diff.
 *
 * **One literal covers every version ever released.** `git diff v0.1.0 HEAD` over
 * `core/domain/.../export/`, `Card.kt`, `LogEntry.kt`, `Board.kt` and `MediaAsset.kt` is empty, so
 * the writer that produced this is the writer that produced any `.larova` file in existence —
 * `0.1.0` through `0.4.2`.
 *
 * What it deliberately contains:
 * - tile types spelled as **Kotlin constant names** (`GUIDE`, `APP_LINK`), which is the whole point
 * - `APP_LINK` specifically: the only key whose spelling differs from its constant name by more
 *   than case, so a reader that cheats with `lowercase()` passes on the other nine and fails here
 * - a log kind spelled `CARD_OPENED`, the same defect one model over
 * - a `FOLDER` tile whose payload points at the second board, so a restore can be checked for the
 *   folder still opening its own tiles
 * - a media asset, so the media path is exercised by a legacy file rather than only a fresh one
 */
internal const val LEGACY_CONTENT_JSON: String = """{
  "boards": [
    {
      "id": "11111111-1111-4111-8111-111111111111",
      "parentId": null,
      "title": "Start",
      "sortIndex": 0,
      "updatedAt": "2026-02-25T06:13:20Z"
    },
    {
      "id": "22222222-2222-4222-8222-222222222222",
      "parentId": "11111111-1111-4111-8111-111111111111",
      "title": "Mornings",
      "sortIndex": 1,
      "updatedAt": "2026-02-25T06:13:20Z"
    }
  ],
  "cards": [
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-000000000001",
      "boardId": "11111111-1111-4111-8111-111111111111",
      "title": "Bedtime",
      "subtitle": null,
      "icon": "star",
      "colorToken": "sand",
      "sortIndex": 1,
      "visibleToCaregiver": true,
      "type": "GUIDE",
      "payload": "{\"type\":\"guide\",\"steps\":[{\"text\":\"Teeth.\"}]}",
      "locale": null,
      "updatedAt": "2026-02-25T06:13:20Z"
    },
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-000000000002",
      "boardId": "11111111-1111-4111-8111-111111111111",
      "title": "Allergies",
      "subtitle": null,
      "icon": "star",
      "colorToken": "sand",
      "sortIndex": 2,
      "visibleToCaregiver": true,
      "type": "NOTE",
      "payload": "{\"type\":\"note\",\"text\":\"No nuts.\"}",
      "locale": null,
      "updatedAt": "2026-02-25T06:13:20Z"
    },
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-000000000003",
      "boardId": "11111111-1111-4111-8111-111111111111",
      "title": "Packing",
      "subtitle": null,
      "icon": "star",
      "colorToken": "sand",
      "sortIndex": 3,
      "visibleToCaregiver": true,
      "type": "CHECKLIST",
      "payload": "{\"type\":\"checklist\",\"items\":[{\"text\":\"Cap\",\"done\":false}],\"resetDaily\":true}",
      "locale": null,
      "updatedAt": "2026-02-25T06:13:20Z"
    },
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-000000000004",
      "boardId": "11111111-1111-4111-8111-111111111111",
      "title": "Bus",
      "subtitle": null,
      "icon": "star",
      "colorToken": "sand",
      "sortIndex": 4,
      "visibleToCaregiver": true,
      "type": "TABLE",
      "payload": "{\"type\":\"table\",\"columns\":[\"Line\",\"Time\"],\"rows\":[[\"6\",\"07:40\"]]}",
      "locale": null,
      "updatedAt": "2026-02-25T06:13:20Z"
    },
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-000000000005",
      "boardId": "11111111-1111-4111-8111-111111111111",
      "title": "Grandma",
      "subtitle": null,
      "icon": "star",
      "colorToken": "sand",
      "sortIndex": 5,
      "visibleToCaregiver": true,
      "type": "PHONE",
      "payload": "{\"type\":\"phone\",\"contacts\":[{\"displayName\":\"Oma\",\"number\":\"+4930123456\",\"inHelpSheet\":true}]}",
      "locale": null,
      "updatedAt": "2026-02-25T06:13:20Z"
    },
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-000000000006",
      "boardId": "11111111-1111-4111-8111-111111111111",
      "title": "Weather",
      "subtitle": null,
      "icon": "star",
      "colorToken": "sand",
      "sortIndex": 6,
      "visibleToCaregiver": true,
      "type": "WEB",
      "payload": "{\"type\":\"web\",\"url\":\"https://example.org\",\"label\":\"Weather\"}",
      "locale": null,
      "updatedAt": "2026-02-25T06:13:20Z"
    },
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-000000000007",
      "boardId": "11111111-1111-4111-8111-111111111111",
      "title": "Music",
      "subtitle": null,
      "icon": "star",
      "colorToken": "sand",
      "sortIndex": 7,
      "visibleToCaregiver": true,
      "type": "APP_LINK",
      "payload": "{\"type\":\"appLink\",\"packageName\":\"com.example.music\",\"label\":\"Music\"}",
      "locale": null,
      "updatedAt": "2026-02-25T06:13:20Z"
    },
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-000000000008",
      "boardId": "11111111-1111-4111-8111-111111111111",
      "title": "Mornings",
      "subtitle": null,
      "icon": "star",
      "colorToken": "sand",
      "sortIndex": 8,
      "visibleToCaregiver": true,
      "type": "FOLDER",
      "payload": "{\"type\":\"folder\",\"boardId\":\"22222222-2222-4222-8222-222222222222\"}",
      "locale": null,
      "updatedAt": "2026-02-25T06:13:20Z"
    }
  ],
  "media": [
    {
      "id": "bbbbbbbb-bbbb-4bbb-8bbb-000000000001",
      "relativePath": "media/bbbbbbbb-bbbb-4bbb-8bbb-000000000001.jpg",
      "mimeType": "image/jpeg",
      "sizeBytes": 2048,
      "sha256": "deadbeef"
    }
  ],
  "log": [
    {
      "id": "cccccccc-cccc-4ccc-8ccc-000000000001",
      "at": "2026-02-25T06:13:20Z",
      "kind": "CARD_OPENED",
      "cardId": "aaaaaaaa-aaaa-4aaa-8aaa-000000000001",
      "note": null
    },
    {
      "id": "cccccccc-cccc-4ccc-8ccc-000000000002",
      "at": "2026-02-25T06:13:20Z",
      "kind": "CHECK_TOGGLED",
      "cardId": "aaaaaaaa-aaaa-4aaa-8aaa-000000000003",
      "note": "Cap"
    }
  ]
}"""

/** The manifest that shipped alongside it: schema 1, and the app version that wrote it. */
internal const val LEGACY_APP_VERSION: String = "0.4.2"

internal const val LEGACY_SCHEMA_VERSION: Int = 1

/** The eight tile titles in [LEGACY_CONTENT_JSON], for asserting nothing was lost on import. */
internal val LEGACY_CARD_TITLES: List<String> = listOf(
    "Bedtime", "Allergies", "Packing", "Bus", "Grandma", "Weather", "Music", "Mornings",
)
