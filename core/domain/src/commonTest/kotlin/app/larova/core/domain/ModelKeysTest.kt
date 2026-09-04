package app.larova.core.domain

import app.larova.core.domain.export.ExportBoard
import app.larova.core.domain.export.ExportCard
import app.larova.core.domain.export.toDomain
import app.larova.core.domain.export.toDomainOrNull
import app.larova.core.domain.export.toExport
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

    /**
     * The **constant names** are part of the file format too, and that is not obvious.
     *
     * Every backup written between `0.1.0` and `0.4.2` spells tile types and log kinds as these
     * identifiers, because the container serialized the enums directly. The reader in
     * `ExportRows.kt` therefore accepts them and always will, which makes a rename a silent
     * format break: renaming `APP_LINK` would compile, pass every other test, and stop every
     * pre-`0.5.0` backup from restoring its shortcut tiles.
     *
     * So both spellings are frozen. If this test is in your way, the answer is a new constant, not
     * a renamed one.
     */
    @Test
    fun theConstantNamesAreFrozenBecauseOldFilesSpellThemThatWay() {
        assertEquals(
            listOf(
                "GUIDE",
                "NOTE",
                "CHECKLIST",
                "TABLE",
                "VIDEO",
                "AUDIO",
                "PHONE",
                "WEB",
                "APP_LINK",
                "FOLDER",
            ),
            CardType.entries.map { it.name },
        )
        assertEquals(
            listOf("CARD_OPENED", "CHECK_TOGGLED", "CALL_PREPARED", "MANUAL_NOTE"),
            LogKind.entries.map { it.name },
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

    /**
     * A UUID that started being written as a byte array, or a timestamp that lost its offset, would
     * make an older backup unreadable without anyone noticing at the time.
     *
     * Asserted on [ExportCard] rather than on [Card], because [Card] is no longer serializable at
     * all — the whole point of the wire types. Going through [toExport] and back through
     * [toDomainOrNull] checks the two mappings at the same time, which the old version of this test
     * could not: it serialized the domain model directly, so a mapping that dropped a field would
     * have passed.
     */
    @Test
    fun idsAndTimestampsAreWrittenAsStrings() {
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

        val encoded = json.encodeToString(card.toExport())

        assertTrue(encoded.contains("\"id\":\"11111111-2222-4333-8444-555555555555\""), encoded)
        assertTrue(encoded.contains("\"updatedAt\":\"2026-08-23T18:12:00Z\""), encoded)
        assertEquals(card, json.decodeFromString<ExportCard>(encoded).toDomainOrNull())
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
        // A missing parent has to survive the file as *absent*, not as the string "null".
        val encoded = json.encodeToString(board.toExport())
        assertEquals(board, json.decodeFromString<ExportBoard>(encoded).toDomain())
    }
}
