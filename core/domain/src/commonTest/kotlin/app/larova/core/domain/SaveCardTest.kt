package app.larova.core.domain

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.Step
import app.larova.core.domain.usecase.CardDraft
import app.larova.core.domain.usecase.DeleteCard
import app.larova.core.domain.usecase.SaveCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalUuidApi::class)
class SaveCardTest {

    private val at = Instant.parse("2026-08-23T18:12:00Z")
    private val boardId = Uuid.parse("66666666-7777-4888-8999-aaaaaaaaaaaa")

    @Test
    fun aNewTileGoesToTheEndOfTheBoard() = runTest {
        // Anything else would move the tiles a caregiver has already learned the position of.
        val cards = FakeCardRepository(listOf(existing("First", 0), existing("Second", 1)))
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        val result = save(draft(title = "Third"))

        assertIs<SaveCard.Result.Saved>(result)
        assertEquals(2, cards.cards.value.first { it.id == result.id }.sortIndex)
    }

    @Test
    fun theTypeComesFromThePayloadRatherThanBesideIt() = runTest {
        // A row claiming to be a checklist with a guide inside it would pass every test here and
        // fail on the screen of whoever opened it.
        val cards = FakeCardRepository()
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        save(draft(title = "Bedtime", payload = CardPayload.Guide(listOf(Step("Teeth")))))

        val stored = cards.cards.value.single()
        assertEquals(CardType.GUIDE, stored.type)
        assertTrue(stored.payload.contains("\"type\":\"guide\""))
    }

    @Test
    fun aTileWithNoTitleIsRefused() = runTest {
        val cards = FakeCardRepository()
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        assertEquals(SaveCard.Result.TitleMissing, save(draft(title = "   ")))
        assertTrue(cards.cards.value.isEmpty())
    }

    @Test
    fun withNoStartScreenNothingIsWritten() = runTest {
        // Refusing beats writing a row on a board that does not exist, which nothing could show.
        val cards = FakeCardRepository()
        val save = SaveCard(cards, FakeBoardRepository())

        assertEquals(SaveCard.Result.NoBoard, save(draft(title = "Bedtime")))
        assertTrue(cards.cards.value.isEmpty())
    }

    @Test
    fun editingKeepsThePositionAndTheIdentity() = runTest {
        // A tile that jumped to the end of the grid every time a typo was fixed would be unusable.
        val stored = existing("Bedtime", sortIndex = 3)
        val cards = FakeCardRepository(listOf(stored))
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        val result = save(draft(id = stored.id, title = "Bedtime routine"))

        assertIs<SaveCard.Result.Saved>(result)
        assertEquals(stored.id, result.id)
        val after = cards.cards.value.single()
        assertEquals(3, after.sortIndex)
        assertEquals("Bedtime routine", after.title)
        assertEquals(1, cards.cards.value.size)
    }

    @Test
    fun titlesAreTrimmedAndAnEmptySecondLineBecomesNothing() = runTest {
        val cards = FakeCardRepository()
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        save(draft(title = "  Bedtime  ", subtitle = "   "))

        val stored = cards.cards.value.single()
        assertEquals("Bedtime", stored.title)
        assertNull(stored.subtitle)
    }

    @Test
    fun theKeysArePassedThroughUntouched() = runTest {
        // The editor picks a token and a symbol key. Neither is resolved, validated or replaced on
        // the way in: a colour this version does not know still belongs to the parent who chose it.
        val cards = FakeCardRepository()
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        save(draft(title = "Tile", colorToken = "aubergine", icon = "hologram"))

        val stored = cards.cards.value.single()
        assertEquals("aubergine", stored.colorToken)
        assertEquals("hologram", stored.icon)
    }

    @Test
    fun deletingRemovesTheTileAndSaysWhetherItWasThere() = runTest {
        val stored = existing("Bedtime", 0)
        val cards = FakeCardRepository(listOf(stored))
        val delete = DeleteCard(cards)

        assertTrue(delete(stored.id))
        assertTrue(cards.cards.value.isEmpty())
        // Deleting the same tile twice — a double tap, or a stale screen.
        assertFalse(delete(stored.id))
    }

    private fun root() = Board(id = boardId, parentId = null, title = "", sortIndex = 0, updatedAt = at)

    private fun draft(
        title: String,
        id: Uuid? = null,
        subtitle: String? = null,
        colorToken: String = "sand",
        icon: String = "star",
        payload: CardPayload = CardPayload.Note("Text"),
    ) = CardDraft(
        id = id,
        title = title,
        subtitle = subtitle,
        colorToken = colorToken,
        icon = icon,
        payload = payload,
    )

    private fun existing(title: String, sortIndex: Int) = Card(
        id = Uuid.random(),
        boardId = boardId,
        title = title,
        icon = "star",
        colorToken = "sand",
        sortIndex = sortIndex,
        type = CardType.NOTE,
        payload = CardPayloadCodec.encode(CardPayload.Note("Text")),
        updatedAt = at,
    )
}
