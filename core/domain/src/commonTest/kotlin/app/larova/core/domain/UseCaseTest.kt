package app.larova.core.domain

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.Step
import app.larova.core.domain.usecase.EnsureRootBoard
import app.larova.core.domain.usecase.ObserveHomeTiles
import app.larova.core.domain.usecase.ObserveTile
import app.larova.core.domain.usecase.ToggleChecklistItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalUuidApi::class)
class UseCaseTest {

    private val at = Instant.parse("2026-08-23T18:12:00Z")
    private val boardId = Uuid.parse("66666666-7777-4888-8999-aaaaaaaaaaaa")

    @Test
    fun aFreshInstallationGetsAStartScreen() = runTest {
        val boards = FakeBoardRepository()

        val board = EnsureRootBoard(boards)()

        assertNull(board.parentId)
        assertEquals(1, boards.boards.value.size)
    }

    @Test
    fun theStartScreenIsNotCreatedTwice() = runTest {
        // Called on every launch, so this is the case that matters: a second board with no parent
        // would give the app two start screens and no way to tell which one holds the tiles.
        val boards = FakeBoardRepository()
        val ensure = EnsureRootBoard(boards)

        val first = ensure()
        val second = ensure()

        assertEquals(first.id, second.id)
        assertEquals(1, boards.upsertCount)
    }

    @Test
    fun theGridShowsTilesInTheirStoredOrder() = runTest {
        val boards = FakeBoardRepository(listOf(rootBoard()))
        val cards = FakeCardRepository(
            listOf(
                card("Bedtime", sortIndex = 1),
                card("Food", sortIndex = 0),
            ),
        )

        val tiles = ObserveHomeTiles(boards, cards)().first()

        assertEquals(listOf("Food", "Bedtime"), tiles.map { it.card.title })
    }

    @Test
    fun aTileWithAPayloadThisVersionCannotReadIsLeftOutRatherThanBreakingTheGrid() = runTest {
        val boards = FakeBoardRepository(listOf(rootBoard()))
        val cards = FakeCardRepository(
            listOf(
                card("Readable", sortIndex = 0),
                card("From the future", sortIndex = 1, payload = """{"type":"hologram"}"""),
            ),
        )

        val tiles = ObserveHomeTiles(boards, cards)().first()

        assertEquals(listOf("Readable"), tiles.map { it.card.title })
        // Still in the database: an export from here has to carry it, and a later version has to be
        // able to render it.
        assertEquals(2, cards.cards.value.size)
    }

    @Test
    fun anInstallationWithNoStartScreenYetShowsNothingRatherThanFailing() = runTest {
        val tiles = ObserveHomeTiles(FakeBoardRepository(), FakeCardRepository())().first()
        assertTrue(tiles.isEmpty())
    }

    @Test
    fun openingATileByAnIdentifierThatIsNotOneIsAnsweredWithNull() = runTest {
        val cards = FakeCardRepository(listOf(card("Bedtime", sortIndex = 0)))
        val observe = ObserveTile(cards)

        // A stale back stack entry, or a route that was never a card id.
        assertNull(observe("not-a-uuid"))
        assertNull(observe(""))
        assertNull(observe(Uuid.random().toString()))
        assertNotNull(observe(cards.cards.value.single().id.toString()))
    }

    @Test
    fun tickingAnItemPersistsThroughThePayload() = runTest {
        val checklist = CardPayload.Checklist(
            listOf(CheckItem("Teeth"), CheckItem("Pyjamas", done = true)),
        )
        val stored = card("Evening", sortIndex = 0, payload = CardPayloadCodec.encode(checklist))
        val cards = FakeCardRepository(listOf(stored))

        assertTrue(ToggleChecklistItem(cards)(stored.id, 0))

        val after = CardPayloadCodec.decodeOrNull(cards.cards.value.single().payload)
        assertEquals(
            listOf(true, true),
            (after as CardPayload.Checklist).items.map { it.done },
        )
    }

    @Test
    fun tickingTwiceReturnsTheItemToWhereItStarted() = runTest {
        val checklist = CardPayload.Checklist(listOf(CheckItem("Teeth")))
        val stored = card("Evening", sortIndex = 0, payload = CardPayloadCodec.encode(checklist))
        val cards = FakeCardRepository(listOf(stored))
        val toggle = ToggleChecklistItem(cards)

        toggle(stored.id, 0)
        toggle(stored.id, 0)

        val after = CardPayloadCodec.decodeOrNull(cards.cards.value.single().payload)
        assertFalse((after as CardPayload.Checklist).items.single().done)
    }

    @Test
    fun tickingSomethingThatIsNotThereChangesNothingAndSaysSo() = runTest {
        // A stale screen: the tile may have been edited on the other side of the app. False rather
        // than an exception, and above all not a write that invents an item.
        val checklist = CardPayload.Checklist(listOf(CheckItem("Teeth")))
        val stored = card("Evening", sortIndex = 0, payload = CardPayloadCodec.encode(checklist))
        val cards = FakeCardRepository(listOf(stored))
        val toggle = ToggleChecklistItem(cards)

        assertFalse(toggle(stored.id, 4))
        assertFalse(toggle(stored.id, -1))
        assertFalse(toggle(Uuid.random(), 0))
        assertEquals(stored.payload, cards.cards.value.single().payload)
    }

    @Test
    fun tickingAnItemOnATileThatIsNotAChecklistIsRefused() = runTest {
        val guide = CardPayloadCodec.encode(CardPayload.Guide(listOf(Step("Brush teeth"))))
        val stored = card("Bedtime", sortIndex = 0, payload = guide)
        val cards = FakeCardRepository(listOf(stored))

        assertFalse(ToggleChecklistItem(cards)(stored.id, 0))
        assertEquals(guide, cards.cards.value.single().payload)
    }

    private fun rootBoard() = Board(id = boardId, parentId = null, title = "", sortIndex = 0, updatedAt = at)

    private fun card(
        title: String,
        sortIndex: Int,
        payload: String = CardPayloadCodec.encode(CardPayload.Note("Something")),
    ) = Card(
        id = Uuid.random(),
        boardId = boardId,
        title = title,
        icon = "star",
        colorToken = "sand",
        sortIndex = sortIndex,
        type = CardType.NOTE,
        payload = payload,
        updatedAt = at,
    )
}
