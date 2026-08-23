package app.larova.core.data

import app.larova.core.data.db.CardEntity
import app.larova.core.data.db.LogEntryEntity
import app.larova.core.data.db.toDomainOrNull
import app.larova.core.data.db.toEntity
import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.LogKind
import app.larova.core.domain.model.MediaAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The mapping between rows and models is where a stored key could quietly become something else,
 * so it is tested in both directions and against rows that cannot be read at all.
 */
@OptIn(ExperimentalUuidApi::class)
class MappersTest {

    private val boardId = Uuid.parse("66666666-7777-4888-8999-aaaaaaaaaaaa")
    private val cardId = Uuid.parse("11111111-2222-4333-8444-555555555555")
    private val at = Instant.parse("2026-08-23T18:12:00Z")

    @Test
    fun aBoardSurvivesARoundTrip() {
        val board = Board(id = boardId, parentId = null, title = "Start", sortIndex = 0, updatedAt = at)
        assertEquals(board, board.toEntity().toDomainOrNull())
    }

    @Test
    fun aCardSurvivesARoundTripWithItsKeysIntact() {
        val card = Card(
            id = cardId,
            boardId = boardId,
            title = "Bedtime",
            subtitle = "Five steps",
            icon = "moon",
            colorToken = "sage",
            sortIndex = 3,
            visibleToCaregiver = true,
            type = CardType.GUIDE,
            payload = """{"type":"guide","steps":[]}""",
            locale = null,
            updatedAt = at,
        )
        val row = card.toEntity()
        // The columns hold the keys, not ordinals: an ordinal moves the moment a value is inserted
        // into the middle of an enum, and every stored tile becomes a different one.
        assertEquals("guide", row.type)
        assertEquals("sage", row.colorToken)
        assertEquals("moon", row.icon)
        assertEquals(card, row.toDomainOrNull())
    }

    @Test
    fun timestampsKeepTheirInstantThroughTheDatabase() {
        val entry = LogEntry(id = cardId, at = at, kind = LogKind.CARD_OPENED, cardId = boardId, note = null)
        val round = entry.toEntity().toDomainOrNull()
        assertEquals(entry, round)
        assertEquals(at, round?.at)
    }

    @Test
    fun aMediaAssetSurvivesARoundTrip() {
        val asset = MediaAsset(
            id = cardId,
            relativePath = "media/$cardId.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 91_234,
            sha256 = "a".repeat(64),
        )
        assertEquals(asset, asset.toEntity().toDomainOrNull())
    }

    @Test
    fun aRowWithAnUnknownTileTypeIsSkippedRatherThanGuessedAt() {
        // What a tile written by a newer version looks like from here. The row stays in the table,
        // so an export from this version still carries it; it simply is not rendered.
        val row = CardEntity(
            id = cardId.toString(),
            boardId = boardId.toString(),
            title = "Hologram",
            subtitle = null,
            icon = "sparkle",
            colorToken = "sky",
            sortIndex = 0,
            visibleToCaregiver = true,
            type = "hologram",
            payload = """{"type":"hologram"}""",
            locale = null,
            updatedAtEpochMillis = at.toEpochMilliseconds(),
        )
        assertNull(row.toDomainOrNull())
    }

    @Test
    fun aRowWithAnIdentifierThatIsNotAUuidIsSkipped() {
        val row = LogEntryEntity(
            id = "not-a-uuid",
            atEpochMillis = at.toEpochMilliseconds(),
            kind = "cardOpened",
            cardId = null,
            note = null,
        )
        assertNull(row.toDomainOrNull())
    }

    @Test
    fun anUnknownColourKeyIsCarriedThroughUntouched() {
        // The fallback to the default belongs to the theme, not to the database. Overwriting the
        // stored key here would turn "a colour this version does not know" into data loss.
        val card = Card(
            id = cardId,
            boardId = boardId,
            title = "Tile",
            icon = "star",
            colorToken = "aubergine",
            sortIndex = 0,
            type = CardType.NOTE,
            payload = """{"type":"note","text":""}""",
            updatedAt = at,
        )
        assertEquals("aubergine", card.toEntity().colorToken)
        assertEquals("aubergine", card.toEntity().toDomainOrNull()?.colorToken)
    }
}
