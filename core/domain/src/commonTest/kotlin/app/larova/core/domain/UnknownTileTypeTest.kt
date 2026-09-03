package app.larova.core.domain

import app.larova.core.domain.export.ExportCodec
import app.larova.core.domain.export.ExportContent
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerializationException

/**
 * What an older build actually does with a package containing a tile type it has never heard of.
 *
 * `Card.payload` is a raw `String` precisely so a newer payload shape cannot bring an import down,
 * and `CardType.fromKey` returns null so an unrecognised type is skipped rather than guessed at.
 * Those two together are meant to make a future tile type a per-tile miss.
 *
 * `Card.type` is the enum itself, though, and these tests pin down what that costs at the file
 * boundary — where the decision is all-or-nothing rather than per tile.
 */
@OptIn(ExperimentalUuidApi::class)
class UnknownTileTypeTest {

    private val boardId = Uuid.random()
    private val at = Instant.fromEpochMilliseconds(1_700_000_000_000)

    /** The documented promise: a payload from the future is carried, not rejected. */
    @Test
    fun aPayloadShapeFromTheFutureSurvivesTheRoundTrip() {
        val json = ExportCodec.json.encodeToString(content(payload = """{"type":"guide"}"""))
            .replace(
                """{\"type\":\"guide\"}""",
                """{\"type\":\"guide\",\"somethingNewEntirely\":42}""",
            )

        val decoded = ExportCodec.json.decodeFromString<ExportContent>(json)

        assertEquals(1, decoded.cards.size)
        assertNotNull(decoded.cards.single().payload)
    }

    /**
     * And the cost: an unrecognised *type* is not a per-tile miss. `Card.type` is the enum, the
     * export `Json` sets no `coerceInputValues`, and the property has no default — so the whole
     * `content.json` fails to decode, not the one card.
     *
     * `ImportPackage.decodeOrNull` turns that into `Result.Unreadable`, which the transfer screen
     * shows as "not a Larova package at all". The file is a perfectly good Larova package written
     * by a newer version, and the person is told the wrong thing about it.
     */
    @Test
    fun anUnknownTileTypeFailsTheWholeContentRatherThanOneCard() {
        val json = ExportCodec.json
            .encodeToString(content(payload = """{"type":"guide"}"""))
            .replace("\"GUIDE\"", "\"TIMETABLE\"")

        val thrown = runCatching { ExportCodec.json.decodeFromString<ExportContent>(json) }
            .exceptionOrNull()

        assertNotNull(thrown, "an unknown type is expected to fail the decode")
        assertEquals(true, thrown is SerializationException)
    }

    /**
     * What the export file actually writes for a tile type: the **enum constant name**, not the
     * frozen `key`.
     *
     * `CardType` carries no `@SerialName`, so kotlinx uses its default enum serializer and the
     * file says `GUIDE` while the database column says `guide` and the payload discriminator says
     * `guide`. The doc comment on `CardType` claims the key is "the value stored in the database
     * and in every export file"; only the first half is true.
     *
     * This test exists to make that a decision rather than an accident. The export format is
     * currently pinned to Kotlin identifiers, so renaming `APP_LINK` to `APP` would change the
     * file format for every tile of that kind and no compiler would object.
     */
    @Test
    fun theFileWritesTheEnumNameRatherThanTheFrozenKey() {
        val json = ExportCodec.json.encodeToString(content(payload = "{}"))

        assertEquals(true, json.contains("\"type\": \"GUIDE\""), json)
        assertEquals(false, json.contains("\"type\": \"guide\""), json)
        // The frozen key, for contrast - what the database and the payload discriminator use.
        assertEquals("guide", CardType.GUIDE.key)
    }

    /** The read-out-of-the-database side does behave per tile, which is where fromKey is used. */
    @Test
    fun anUnknownKeyResolvesToNothingRatherThanToTheWrongType() {
        assertNull(CardType.fromKey("timetable"))
        assertNull(CardType.fromKey(null))
        assertEquals(CardType.GUIDE, CardType.fromKey("guide"))
    }

    private fun content(payload: String) = ExportContent(
        cards = listOf(
            Card(
                id = Uuid.random(),
                boardId = boardId,
                title = "Bedtime",
                icon = "moon",
                colorToken = "sage",
                sortIndex = 0,
                type = CardType.GUIDE,
                payload = payload,
                updatedAt = at,
            ),
        ),
    )
}
