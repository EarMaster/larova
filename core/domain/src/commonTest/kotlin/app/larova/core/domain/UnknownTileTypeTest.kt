package app.larova.core.domain

import app.larova.core.domain.export.ExportCard
import app.larova.core.domain.export.ExportCodec
import app.larova.core.domain.export.ExportContent
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.LogKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * What a build does with a package containing a tile type or log kind it has never heard of.
 *
 * **What this used to be.** These tests were written to record a defect and now record its
 * absence. `ExportContent` used to serialize the domain models, so `Card.type` was the enum
 * itself — no default, and no `coerceInputValues` on the codec — and a single unrecognised value
 * failed the decode of the *entire* `content.json`. `ImportPackage` reported that as `Unreadable`
 * and the transfer screen said "This is not a Larova backup" about a perfectly good backup written
 * by a newer version. The same code wrote `"type": "GUIDE"`, the Kotlin constant name, pinning the
 * file format to identifier spelling that nothing pinned back.
 *
 * Both enums carried doc comments promising per-row tolerance. These tests are what make those
 * comments true rather than aspirational.
 */
@OptIn(ExperimentalUuidApi::class)
class UnknownTileTypeTest {

    private val boardId = Uuid.random()
    private val at = Instant.fromEpochMilliseconds(1_700_000_000_000)

    /** A payload from the future is carried through untouched — that always worked. */
    @Test
    fun aPayloadShapeFromTheFutureSurvivesTheRoundTrip() {
        val payload = """{"type":"guide","somethingNewEntirely":42}"""

        val decoded = ExportCodec.decodeContentOrNull(
            ExportCodec.json.encodeToString(content(type = "guide", payload = payload)),
        )

        assertNotNull(decoded)
        assertEquals(payload, decoded.cards.single().payload)
        assertEquals(0, decoded.skippedCards)
    }

    /** The fix: one unfamiliar tile costs that tile, and the count reaches the caller. */
    @Test
    fun anUnknownTileTypeCostsOneTileRatherThanTheFile() {
        val json = ExportCodec.json.encodeToString(
            ExportContent(
                cards = listOf(
                    card(type = "guide", title = "Bedtime"),
                    card(type = "timetable", title = "From a newer Larova"),
                    card(type = "note", title = "Allergies"),
                ),
            ),
        )

        val decoded = ExportCodec.decodeContentOrNull(json)

        assertNotNull(decoded, "an unknown type must no longer fail the whole file")
        assertEquals(2, decoded.cards.size)
        assertEquals(1, decoded.skippedCards)
        assertEquals(listOf("Bedtime", "Allergies"), decoded.cards.map { it.title })
    }

    /** Same tolerance one model over, where the comment made the same promise. */
    @Test
    fun anUnknownLogKindCostsOneLineRatherThanTheFile() {
        val json = ExportCodec.json.encodeToString(content(type = "guide"))
            .replace("\"log\": []", LOG_WITH_ONE_UNKNOWN_KIND)

        val decoded = ExportCodec.decodeContentOrNull(json)

        assertNotNull(decoded)
        assertEquals(1, decoded.log.size)
        assertEquals(LogKind.CARD_OPENED, decoded.log.single().kind)
        assertEquals(1, decoded.skippedLogEntries)
    }

    /** Genuinely broken JSON is still refused outright. Tolerance is per row, not per file. */
    @Test
    fun unreadableJsonIsStillRefused() {
        assertNull(ExportCodec.decodeContentOrNull("not json at all"))
        assertNull(ExportCodec.decodeContentOrNull("""{"cards": "should be a list"}"""))
    }

    /**
     * The file writes the frozen key, matching the database column and the payload discriminator.
     *
     * The inverse of what this file once asserted. One concept used to have three spellings.
     */
    @Test
    fun theFileWritesTheFrozenKey() {
        val json = ExportCodec.json.encodeToString(content(type = "appLink"))

        assertTrue(json.contains("\"type\": \"appLink\""), json)
        assertTrue(!json.contains("APP_LINK"), json)
        assertEquals("appLink", CardType.APP_LINK.key)
    }

    /** Reading out of the database is per row too, which is where `fromKey` has always been used. */
    @Test
    fun anUnknownKeyResolvesToNothingRatherThanToTheWrongType() {
        assertNull(CardType.fromKey("timetable"))
        assertNull(CardType.fromKey(null))
        assertEquals(CardType.GUIDE, CardType.fromKey("guide"))
        assertNull(LogKind.fromKey("somethingElse"))
    }

    private fun content(type: String, payload: String = "{}") =
        ExportContent(cards = listOf(card(type = type, payload = payload)))

    private fun card(type: String, title: String = "Bedtime", payload: String = "{}") = ExportCard(
        id = Uuid.random(),
        boardId = boardId,
        title = title,
        icon = "moon",
        colorToken = "sage",
        sortIndex = 0,
        type = type,
        payload = payload,
        updatedAt = at,
    )

    private companion object {
        /** One kind this build knows and one it does not, spliced into an otherwise valid file. */
        val LOG_WITH_ONE_UNKNOWN_KIND = """
            "log": [
                {
                  "id": "cccccccc-cccc-4ccc-8ccc-000000000001",
                  "at": "2026-02-25T06:13:20Z",
                  "kind": "cardOpened"
                },
                {
                  "id": "cccccccc-cccc-4ccc-8ccc-000000000002",
                  "at": "2026-02-25T06:13:20Z",
                  "kind": "moodRecorded"
                }
            ]
        """.trimIndent()
    }
}
