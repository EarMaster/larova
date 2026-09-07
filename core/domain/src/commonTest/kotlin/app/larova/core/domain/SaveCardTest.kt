package app.larova.core.domain

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.Step
import app.larova.core.domain.usecase.CardDraft
import app.larova.core.domain.usecase.CreateFolderBoard
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
        val delete = DeleteCard(cards, FakeBoardRepository(listOf(root())))

        assertTrue(delete(stored.id))
        assertTrue(cards.cards.value.isEmpty())
        // Deleting the same tile twice — a double tap, or a stale screen.
        assertFalse(delete(stored.id))
    }

    /**
     * A folder owns its board. Deleting the tile and leaving the board behind would leave the tiles
     * on it in the database with no way to reach them: not on the start screen, not in a folder,
     * and still in every backup from then on.
     */
    @Test
    fun deletingAFolderTakesTheTilesInsideItWithIt() = runTest {
        val folderBoard = Uuid.random()
        val folderTile = existing("Holidays", 0).copy(
            type = CardType.FOLDER,
            payload = CardPayloadCodec.encode(CardPayload.Folder(boardId = folderBoard)),
        )
        val inside = existing("Beach", 0).copy(boardId = folderBoard)
        val elsewhere = existing("Bedtime", 1)

        val cards = FakeCardRepository(listOf(folderTile, inside, elsewhere))
        val boards = FakeBoardRepository(
            listOf(root(), Board(folderBoard, boardId, "Holidays", 0, at)),
        )

        assertTrue(DeleteCard(cards, boards)(folderTile.id))

        assertEquals(listOf("Bedtime"), cards.cards.value.map { it.title })
        assertEquals(listOf(boardId), boards.boards.value.map { it.id })
    }

    @Test
    fun aNewTileGoesOnTheBoardTheEditorWasOpenedFrom() = runTest {
        val folderBoard = Uuid.random()
        val cards = FakeCardRepository()
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        save(draft(title = "Beach").copy(boardId = folderBoard))

        assertEquals(folderBoard, cards.cards.value.single().boardId)
    }

    /** Saving a tile is not a way to move it: an existing one keeps the board it is already on. */
    @Test
    fun anExistingTileKeepsItsBoard() = runTest {
        val folderBoard = Uuid.random()
        val stored = existing("Beach", 0).copy(boardId = folderBoard)
        val cards = FakeCardRepository(listOf(stored))
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        save(draft(title = "Beach again", id = stored.id).copy(boardId = null))

        assertEquals(folderBoard, cards.cards.value.single().boardId)
    }

    @Test
    fun aFolderBoardHangsOffTheStartScreen() = runTest {
        val boards = FakeBoardRepository(listOf(root(), Board(Uuid.random(), boardId, "First", 0, at)))

        val created = CreateFolderBoard(boards)("Holidays")

        val board = boards.boards.value.single { it.id == created }
        assertEquals(boardId, board.parentId)
        assertEquals("Holidays", board.title)
        // After the folder that is already there, not on top of it.
        assertEquals(1, board.sortIndex)
    }

    /** No start screen means no parent to hang it off, and a board with no parent is a second one. */
    @Test
    fun aFolderBoardNeedsAStartScreen() = runTest {
        assertNull(CreateFolderBoard(FakeBoardRepository())("Holidays"))
    }

    private fun root() = Board(id = boardId, parentId = null, title = "", sortIndex = 0, updatedAt = at)


    @Test
    fun theLanguageATileIsWrittenInIsStoredAsItComes() = runTest {
        // The editor is the only thing that can know this — it is asked, never derived — so the
        // one thing this write must not do is lose it. It ends up in every export file.
        val cards = FakeCardRepository()
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        val result = save(draft(title = "Essen und Trinken", locale = "de"))

        assertIs<SaveCard.Result.Saved>(result)
        assertEquals("de", cards.cards.value.first { it.id == result.id }.locale)
    }

    @Test
    fun theLanguageIsCanonicalisedOnTheWayIn() = runTest {
        // `canonicalLanguageTag`'s rule, applied at the write boundary: two spellings of
        // Portuguese must not become two languages that `resolveCardText` then cannot match.
        val cards = FakeCardRepository()
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        val result = save(draft(title = "Comer e beber", locale = "PT-pt"))

        assertIs<SaveCard.Result.Saved>(result)
        assertEquals("pt-PT", cards.cards.value.first { it.id == result.id }.locale)
    }

    @Test
    fun somethingThatIsNotALanguageTagIsNotStored() = runTest {
        // Null is a real answer and means "nobody has said". Rubbish is not a different answer.
        val cards = FakeCardRepository()
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        val result = save(draft(title = "Food", locale = "not a tag"))

        assertIs<SaveCard.Result.Saved>(result)
        assertNull(cards.cards.value.first { it.id == result.id }.locale)
    }

    @Test
    fun anEditThatSaysNothingAboutLanguageClearsIt() = runTest {
        // Deliberate, and the reason the editor round-trips the stored value into its draft: a
        // parent must be able to take back an answer they got wrong, and the editor is the only
        // caller. A helper that quietly kept the old value would make that impossible.
        val stored = existing("Essen", 0).copy(locale = "de")
        val cards = FakeCardRepository(listOf(stored))
        val save = SaveCard(cards, FakeBoardRepository(listOf(root())))

        save(draft(title = "Essen", id = stored.id))

        assertNull(cards.cards.value.first { it.id == stored.id }.locale)
    }

    private fun draft(
        title: String,
        id: Uuid? = null,
        subtitle: String? = null,
        colorToken: String = "sand",
        icon: String = "star",
        payload: CardPayload = CardPayload.Note("Text"),
        locale: String? = null,
    ) = CardDraft(
        id = id,
        title = title,
        subtitle = subtitle,
        colorToken = colorToken,
        icon = icon,
        payload = payload,
        locale = locale,
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
