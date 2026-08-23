package app.larova.core.domain

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.usecase.ReorderTiles
import app.larova.core.domain.usecase.SearchTiles
import app.larova.core.domain.usecase.moved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalUuidApi::class)
class ReorderAndSearchTest {

    private val at = Instant.parse("2026-08-23T18:12:00Z")
    private val boardId = Uuid.parse("66666666-7777-4888-8999-aaaaaaaaaaaa")

    @Test
    fun movingAnElementShiftsTheOnesItPasses() {
        assertEquals(listOf("b", "a", "c"), listOf("a", "b", "c").moved(0, 1))
        assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").moved(2, 0))
        assertEquals(listOf("a", "c", "b"), listOf("a", "b", "c").moved(1, 2))
    }

    @Test
    fun aMoveOffTheEndLeavesTheListAlone() {
        // The buttons are disabled at the ends, but a stale screen must not be able to scramble
        // somebody's start screen — and it must not throw either.
        val list = listOf("a", "b", "c")
        assertEquals(list, list.moved(0, -1))
        assertEquals(list, list.moved(2, 3))
        assertEquals(list, list.moved(1, 1))
        assertEquals(list, list.moved(7, 0))
        assertEquals(emptyList(), emptyList<String>().moved(0, 1))
    }

    @Test
    fun theNewOrderIsWrittenInOnePass() = runTest {
        val first = card("First", 0)
        val second = card("Second", 1)
        val third = card("Third", 2)
        val cards = FakeCardRepository(listOf(first, second, third))
        val reorder = ReorderTiles(FakeBoardRepository(listOf(root())), cards)

        assertTrue(reorder(listOf(third.id, first.id, second.id)))

        val order = cards.observeCards(boardId).first().map { it.title }
        assertEquals(listOf("Third", "First", "Second"), order)
    }

    @Test
    fun withNothingToReorderNothingHappens() = runTest {
        val cards = FakeCardRepository(listOf(card("Only", 0)))
        assertFalse(ReorderTiles(FakeBoardRepository(listOf(root())), cards)(emptyList()))
        // No start screen: refusing beats writing an order against a board that does not exist.
        assertFalse(ReorderTiles(FakeBoardRepository(), cards)(listOf(Uuid.random())))
    }

    @Test
    fun searchLooksAtTitlesAndSecondLines() = runTest {
        val cards = FakeCardRepository(
            listOf(
                card("Bedtime", 0),
                card("Food and drink", 1, subtitle = "What he will eat"),
                card("Doctor", 2),
            ),
        )
        val search = SearchTiles(cards)

        assertEquals(listOf("Bedtime"), search("bed").first().map { it.card.title })
        assertEquals(listOf("Food and drink"), search("will eat").first().map { it.card.title })
        // Case is not something a person searching under pressure should have to get right.
        assertEquals(listOf("Doctor"), search("DOC").first().map { it.card.title })
    }

    @Test
    fun anEmptySearchFindsNothingRatherThanEverything() = runTest {
        // The caller shows the ordered start screen instead. "Everything, in title order" is not
        // what an empty search box means.
        val cards = FakeCardRepository(listOf(card("Bedtime", 0)))
        val search = SearchTiles(cards)

        assertTrue(search("").first().isEmpty())
        assertTrue(search("   ").first().isEmpty())
    }

    @Test
    fun searchLeavesOutWhatItCannotRead() = runTest {
        val cards = FakeCardRepository(
            listOf(
                card("Bedtime", 0),
                card("Bedtime story", 1, payload = """{"type":"hologram"}"""),
            ),
        )

        assertEquals(listOf("Bedtime"), SearchTiles(cards)("bed").first().map { it.card.title })
    }

    private fun root() = Board(id = boardId, parentId = null, title = "", sortIndex = 0, updatedAt = at)

    private fun card(
        title: String,
        sortIndex: Int,
        subtitle: String? = null,
        payload: String = CardPayloadCodec.encode(CardPayload.Note("Text")),
    ) = Card(
        id = Uuid.random(),
        boardId = boardId,
        title = title,
        subtitle = subtitle,
        icon = "star",
        colorToken = "sand",
        sortIndex = sortIndex,
        type = CardType.NOTE,
        payload = payload,
        updatedAt = at,
    )
}
