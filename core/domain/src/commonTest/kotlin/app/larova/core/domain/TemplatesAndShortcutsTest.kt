package app.larova.core.domain

import app.larova.core.domain.app.Shortcuts
import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.LogKind
import app.larova.core.domain.usecase.ApplyTemplate
import app.larova.core.domain.usecase.CardDraft
import app.larova.core.domain.usecase.MostOpenedTiles
import app.larova.core.domain.usecase.PublishShortcuts
import app.larova.core.domain.usecase.SHORTCUT_LIMIT
import app.larova.core.domain.usecase.SaveCard
import app.larova.core.domain.usecase.ShortcutTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * The two things that turn a fresh installation into a used one: something on the grid to start
 * from, and the launcher learning what is opened most.
 *
 * The words in a template belong to the UI — a template is written in whatever language the app is
 * in and belongs to the parents from then on — so what is checked here is that a template lands as
 * an ordinary tile, and that the shortcut list is the one the log justifies.
 */
@OptIn(ExperimentalUuidApi::class)
class TemplatesAndShortcutsTest {

    private val boardId = Uuid.parse("11111111-2222-4333-8444-555555555555")

    @Test
    fun aTemplateLandsAsAnOrdinaryTile() = runTest {
        val cards = FakeCardRepository()
        val apply = ApplyTemplate(SaveCard(cards, FakeBoardRepository(listOf(root()))))

        val written = apply(
            listOf(
                CardDraft(
                    title = "Bedtime",
                    colorToken = "sage",
                    icon = "moon",
                    payload = CardPayload.Guide(),
                ),
            ),
        )

        assertEquals(1, written)
        val tile = cards.cards.value.single()
        // Nothing marks it as having come from a template: no flag to filter on later, and no way
        // to "restore" it over something the parents have since written.
        assertEquals("Bedtime", tile.title)
        assertEquals(CardType.GUIDE, tile.type)
        assertEquals("sage", tile.colorToken)
    }

    @Test
    fun aTemplateWithNoTitleIsRefusedLikeAnythingElse() = runTest {
        val cards = FakeCardRepository()
        val apply = ApplyTemplate(SaveCard(cards, FakeBoardRepository(listOf(root()))))

        val written = apply(
            listOf(CardDraft(title = "  ", colorToken = "sand", icon = "star", payload = CardPayload.Note())),
        )

        assertEquals(0, written)
        assertTrue(cards.cards.value.isEmpty())
    }

    @Test
    fun theLauncherGetsTheThreeMostOpenedTiles() = runTest {
        val bedtime = card("Bedtime", sortIndex = 0)
        val lunch = card("Lunch", sortIndex = 1)
        val park = card("Park", sortIndex = 2)
        val museum = card("Museum", sortIndex = 3)
        val cards = FakeCardRepository(listOf(bedtime, lunch, park, museum))
        val log = FakeLogRepository(
            openings(bedtime.id, 5) + openings(lunch.id, 3) + openings(park.id, 2) +
                openings(museum.id, 1),
        )

        val targets = MostOpenedTiles(log, cards)()

        assertEquals(SHORTCUT_LIMIT, targets.size)
        assertEquals(listOf("Bedtime", "Lunch", "Park"), targets.map { it.label })
    }

    /**
     * Two tiles opened the same number of times keep the order the board has them in. A shortcut
     * that swaps places between two equally-used tiles every time the app starts is a shortcut
     * nobody learns.
     */
    @Test
    fun aTieKeepsTheOrderTheBoardHas() = runTest {
        val first = card("Breakfast", sortIndex = 0)
        val second = card("Bath", sortIndex = 1)
        val cards = FakeCardRepository(listOf(second, first))
        val log = FakeLogRepository(openings(first.id, 2) + openings(second.id, 2))

        val targets = MostOpenedTiles(log, cards)()

        assertEquals(listOf("Breakfast", "Bath"), targets.map { it.label })
    }

    @Test
    fun onlyOpeningsCount() = runTest {
        val tile = card("Bedtime", sortIndex = 0)
        val cards = FakeCardRepository(listOf(tile))
        val log = FakeLogRepository(
            listOf(
                entry(LogKind.CHECK_TOGGLED, tile.id, 0),
                entry(LogKind.CALL_PREPARED, tile.id, 1),
                entry(LogKind.MANUAL_NOTE, null, 2),
            ),
        )

        // Ticking something off is not opening it, and a note is not about a tile at all.
        assertTrue(MostOpenedTiles(log, cards)().isEmpty())
    }

    /** A tile deleted since it was opened is not a shortcut: it would open nothing. */
    @Test
    fun aDeletedTileIsNotOffered() = runTest {
        val log = FakeLogRepository(openings(Uuid.random(), 4))

        assertTrue(MostOpenedTiles(log, FakeCardRepository())().isEmpty())
    }

    @Test
    fun publishingReplacesTheWholeList() = runTest {
        val tile = card("Bedtime", sortIndex = 0)
        val cards = FakeCardRepository(listOf(tile))
        val log = FakeLogRepository(openings(tile.id, 1))
        val shortcuts = FakeShortcuts(published = listOf(ShortcutTarget(Uuid.random(), "Gone")))

        PublishShortcuts(MostOpenedTiles(log, cards), shortcuts)()

        assertEquals(listOf("Bedtime"), shortcuts.published.map { it.label })
    }

    @Test
    fun anEmptyLogPublishesNothing() = runTest {
        val shortcuts = FakeShortcuts(published = listOf(ShortcutTarget(Uuid.random(), "Stale")))

        PublishShortcuts(MostOpenedTiles(FakeLogRepository(), FakeCardRepository()), shortcuts)()

        assertTrue(shortcuts.published.isEmpty(), "a stale shortcut survived an empty log")
    }

    private fun openings(cardId: Uuid, times: Int) =
        (0 until times).map { entry(LogKind.CARD_OPENED, cardId, it) }

    private fun entry(kind: LogKind, cardId: Uuid?, minute: Int) = LogEntry(
        id = Uuid.random(),
        at = Clock.System.now() - minute.minutes,
        kind = kind,
        cardId = cardId,
    )

    private fun root() = Board(id = boardId, parentId = null, title = "", sortIndex = 0, updatedAt = AT)

    private fun card(title: String, sortIndex: Int) = Card(
        id = Uuid.random(),
        boardId = boardId,
        title = title,
        icon = "star",
        colorToken = "sand",
        sortIndex = sortIndex,
        type = CardType.NOTE,
        payload = CardPayloadCodec.encode(CardPayload.Note("Text")),
        updatedAt = AT,
    )

    private companion object {
        val AT: Instant = Instant.parse("2026-08-23T18:12:00Z")
    }
}

@OptIn(ExperimentalUuidApi::class)
private class FakeShortcuts(var published: List<ShortcutTarget> = emptyList()) : Shortcuts {

    override suspend fun publish(targets: List<ShortcutTarget>) {
        published = targets
    }
}
