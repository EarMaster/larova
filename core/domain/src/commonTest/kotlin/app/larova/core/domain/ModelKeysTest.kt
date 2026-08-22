package app.larova.core.domain

import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.LogKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json

/**
 * Every string asserted here is written into somebody's export file. Changing one later means
 * migrating data that may exist only in a backup nobody can reach any more.
 */
@OptIn(ExperimentalUuidApi::class)
class ModelKeysTest {

    private val json = Json { prettyPrint = false }

    @Test
    fun theTenCardTypeKeysAreFrozen() {
        assertEquals(
            listOf(
                "guide",
                "note",
                "checklist",
                "table",
                "video",
                "audio",
                "phone",
                "web",
                "appLink",
                "folder",
            ),
            CardType.entries.map { it.key },
        )
    }

    @Test
    fun anUnknownCardTypeIsNullRatherThanASubstitute() {
        assertNull(CardType.fromKey("hologram"))
        assertNull(CardType.fromKey(null))
        assertNull(CardType.fromKey(""))
        for (type in CardType.entries) {
            assertEquals(type, CardType.fromKey(type.key))
        }
    }

    @Test
    fun theLogKindKeysAreFrozen() {
        assertEquals(
            listOf("cardOpened", "checkToggled", "callPrepared", "manualNote"),
            LogKind.entries.map { it.key },
        )
        assertNull(LogKind.fromKey("bloodPressureMeasured"))
    }

    @Test
    fun theAppearanceSettingFallsBackRatherThanFailingToStart() {
        assertEquals(AppearanceSetting.SYSTEM, AppearanceSetting.DEFAULT)
        assertEquals(AppearanceSetting.SYSTEM, AppearanceSetting.fromKey(null))
        assertEquals(AppearanceSetting.SYSTEM, AppearanceSetting.fromKey("sepia"))
        assertEquals(AppearanceSetting.NIGHT, AppearanceSetting.fromKey("night"))
    }

    @Test
    fun idsAndTimestampsAreWrittenAsStrings() {
        // A UUID that started being written as a byte array, or a timestamp that lost its offset,
        // would make an older backup unreadable without anyone noticing at the time.
        val card = Card(
            id = Uuid.parse("11111111-2222-4333-8444-555555555555"),
            boardId = Uuid.parse("66666666-7777-4888-8999-aaaaaaaaaaaa"),
            title = "Bedtime",
            icon = "moon",
            colorToken = "sage",
            sortIndex = 0,
            type = CardType.GUIDE,
            payload = """{"type":"guide","steps":[]}""",
            updatedAt = Instant.parse("2026-08-23T18:12:00Z"),
        )
        val encoded = json.encodeToString(card)
        assertTrue(encoded.contains("\"id\":\"11111111-2222-4333-8444-555555555555\""), encoded)
        assertTrue(encoded.contains("\"updatedAt\":\"2026-08-23T18:12:00Z\""), encoded)
        assertEquals(card, json.decodeFromString<Card>(encoded))
    }

    @Test
    fun aStartScreenBoardHasNoParent() {
        val board = Board(
            id = Uuid.parse("11111111-2222-4333-8444-555555555555"),
            title = "Start",
            sortIndex = 0,
            updatedAt = Instant.parse("2026-08-23T18:12:00Z"),
        )
        assertNull(board.parentId)
        assertEquals(board, json.decodeFromString<Board>(json.encodeToString(board)))
    }
}
