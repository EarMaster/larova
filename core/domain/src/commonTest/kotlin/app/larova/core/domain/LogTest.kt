package app.larova.core.domain

import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.LogKind
import app.larova.core.domain.usecase.ClearLog
import app.larova.core.domain.usecase.LOG_RETENTION_DAYS
import app.larova.core.domain.usecase.ObserveLog
import app.larova.core.domain.usecase.PruneLog
import app.larova.core.domain.usecase.RecordEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * The log records and never interprets.
 *
 * These tests are as much about what is *not* there as what is: no counting, no scoring, no
 * grouping into days with a total at the bottom. That is the line in `docs/concept.md` §2.2, and it
 * is the reason the log can be the documentation feature for the parents without the app becoming
 * something that has to be certified.
 */
@OptIn(ExperimentalUuidApi::class)
class LogTest {

    private val boardId = Uuid.random()

    @Test
    fun theFourThingsWorthRecording() = runTest {
        val log = FakeLogRepository()
        val record = RecordEvent(log)
        val cardId = Uuid.random()

        record.cardOpened(cardId)
        record.checkToggled(cardId)
        record.callPrepared(cardId)
        assertTrue(record.note("  he would not eat lunch  "))

        val kinds = log.entries.value.map { it.kind }
        assertEquals(
            listOf(
                LogKind.CARD_OPENED,
                LogKind.CHECK_TOGGLED,
                LogKind.CALL_PREPARED,
                LogKind.MANUAL_NOTE,
            ),
            kinds,
        )
        // The note is stored as written, trimmed — not reworded, not prefixed with anything.
        assertEquals("he would not eat lunch", log.entries.value.last().note)
        assertNull(log.entries.value.last().cardId)
    }

    @Test
    fun anEmptyNoteIsNotALine() = runTest {
        val log = FakeLogRepository()
        val record = RecordEvent(log)

        assertFalse(record.note(""))
        assertFalse(record.note("   "))
        assertTrue(log.entries.value.isEmpty())
    }

    @Test
    fun theLogReadsNewestFirstWithTheTileNamed() = runTest {
        val tile = card("Bedtime")
        val cards = FakeCardRepository(listOf(tile))
        val log = FakeLogRepository(
            listOf(
                entry(LogKind.CARD_OPENED, at = AT, cardId = tile.id),
                entry(LogKind.MANUAL_NOTE, at = AT + 5.minutes, note = "slept badly"),
            ),
        )

        val lines = ObserveLog(log, cards)().first()

        assertEquals(listOf(LogKind.MANUAL_NOTE, LogKind.CARD_OPENED), lines.map { it.kind })
        assertEquals("Bedtime", lines.last().cardTitle)
        assertEquals("slept badly", lines.first().note)
    }

    /**
     * A deleted tile leaves its entries standing. What happened still happened, and the screen says
     * the tile is gone rather than inventing a name for it.
     */
    @Test
    fun anEntryOutlivesTheTileItRefersTo() = runTest {
        val log = FakeLogRepository(listOf(entry(LogKind.CARD_OPENED, at = AT, cardId = Uuid.random())))

        val line = ObserveLog(log, FakeCardRepository())().first().single()

        assertEquals(LogKind.CARD_OPENED, line.kind)
        assertNull(line.cardTitle)
    }

    /** Renaming a tile does not rewrite history, because the log stores the identifier. */
    @Test
    fun renamingATileRenamesItInTheLogToo() = runTest {
        val tile = card("Bedtime")
        val cards = FakeCardRepository(listOf(tile))
        val log = FakeLogRepository(listOf(entry(LogKind.CARD_OPENED, at = AT, cardId = tile.id)))
        val observe = ObserveLog(log, cards)

        cards.upsert(tile.copy(title = "Going to bed"))

        assertEquals("Going to bed", observe().first().single().cardTitle)
    }

    @Test
    fun whatIsOlderThanTheWindowGoes() = runTest {
        val now = Clock.System.now()
        val log = FakeLogRepository(
            listOf(
                entry(LogKind.MANUAL_NOTE, at = now - 1.days, note = "yesterday"),
                entry(LogKind.MANUAL_NOTE, at = now - (LOG_RETENTION_DAYS + 5).days, note = "last month"),
            ),
        )

        PruneLog(log)()

        assertEquals(listOf("yesterday"), log.entries.value.map { it.note })
    }

    @Test
    fun clearingLeavesNothing() = runTest {
        val log = FakeLogRepository(listOf(entry(LogKind.MANUAL_NOTE, at = AT, note = "a line")))

        ClearLog(log)()

        assertTrue(log.entries.value.isEmpty())
    }

    private fun entry(kind: LogKind, at: Instant, cardId: Uuid? = null, note: String? = null) =
        LogEntry(id = Uuid.random(), at = at, kind = kind, cardId = cardId, note = note)

    private fun card(title: String) = Card(
        id = Uuid.random(),
        boardId = boardId,
        title = title,
        icon = "star",
        colorToken = "sand",
        sortIndex = 0,
        type = CardType.NOTE,
        payload = CardPayloadCodec.encode(CardPayload.Note("Text")),
        updatedAt = AT,
    )

    private companion object {
        val AT: Instant = Instant.parse("2026-08-23T18:12:00Z")
    }
}
