package app.larova.core.data

import app.larova.core.data.db.CardDao
import app.larova.core.data.db.CardEntity
import app.larova.core.data.db.CardTextDao
import app.larova.core.data.db.CardTextEntity
import app.larova.core.data.db.MediaAssetEntity
import app.larova.core.data.db.MediaDao
import app.larova.core.data.repository.RoomMediaRepository
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.Step
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * Media cleanup reads references out of payload JSON, which makes it the one piece of the data
 * layer that can delete a file a guide still shows. Tested against fakes rather than a database,
 * because the logic under test is the reference walk, not SQLite.
 */
@OptIn(ExperimentalUuidApi::class)
class MediaOrphanTest {

    private val used = Uuid.parse("3f2a1b4c-5d6e-4f70-8192-a3b4c5d6e7f8")
    private val alsoUsed = Uuid.parse("91cd1b4c-5d6e-4f70-8192-a3b4c5d6e7f8")
    private val unused = Uuid.parse("00000000-1111-4222-8333-444444444444")

    @Test
    fun onlyUnreferencedAssetsAreRemoved() = runTest {
        val cards = listOf(
            card(CardPayloadCodec.encode(CardPayload.Guide(listOf(Step("Read this", mediaId = used))))),
            card(CardPayloadCodec.encode(CardPayload.Audio(alsoUsed))),
        )
        val media = FakeMediaDao(mutableListOf(asset(used), asset(alsoUsed), asset(unused)))

        val removed = sweep(media, cards)

        assertEquals(1, removed)
        assertEquals(setOf(used.toString(), alsoUsed.toString()), media.rows.map { it.id }.toSet())
    }

    @Test
    fun nothingIsRemovedWhileAPayloadCannotBeRead() {
        // A tile written by a newer version is skipped when rendering, but its files must not be
        // collected underneath it: this version cannot see what that payload refers to, and
        // deleting on a guess is unrecoverable.
        runTest {
            val cards = listOf(card("""{"type":"hologram","projector":"living room"}"""))
            val media = FakeMediaDao(mutableListOf(asset(used), asset(unused)))

            val removed = sweep(media, cards)

            assertEquals(0, removed)
            assertEquals(2, media.rows.size)
        }
    }

    @Test
    fun anEmptyLibraryIsNotAnError() = runTest {
        val media = FakeMediaDao(mutableListOf())
        assertEquals(0, sweep(media, emptyList()))
        assertTrue(media.rows.isEmpty())
    }

    /**
     * A translated guide may point at a different picture — a sign photographed in the caregiver's
     * own language — and the reference walk has to see it.
     *
     * Without this, the first sweep after a family adds their first translation deletes the file,
     * silently and for good. Media is the one thing in this app that cannot be re-derived from
     * anything else.
     */
    @Test
    fun aPictureUsedOnlyByATranslationIsNotCollected() = runTest {
        val tile = card(CardPayloadCodec.encode(CardPayload.Guide(listOf(Step("Read this")))))
        val variant = variant(
            cardId = tile.id,
            payload = CardPayloadCodec.encode(
                CardPayload.Guide(listOf(Step("Oku", mediaId = used))),
            ),
        )
        val media = FakeMediaDao(mutableListOf(asset(used), asset(unused)))

        val removed = sweep(media, listOf(tile), listOf(variant))

        assertEquals(1, removed)
        assertEquals(setOf(used.toString()), media.rows.map { it.id }.toSet())
    }

    /** And the "cannot read it, so do not touch it" guard covers a variant on the same terms. */
    @Test
    fun nothingIsRemovedWhileATranslationsPayloadCannotBeRead() = runTest {
        val tile = card(CardPayloadCodec.encode(CardPayload.Guide(listOf(Step("Read this")))))
        val variant = variant(tile.id, """{"type":"hologram","projector":"living room"}""")
        val media = FakeMediaDao(mutableListOf(asset(used), asset(unused)))

        val removed = sweep(media, listOf(tile), listOf(variant))

        assertEquals(0, removed)
        assertEquals(2, media.rows.size)
    }

    private suspend fun sweep(
        media: FakeMediaDao,
        cards: List<CardEntity>,
        variants: List<CardTextEntity> = emptyList(),
    ) = RoomMediaRepository(media, FakeCardDao(cards), FakeCardTextDao(variants)).deleteOrphans()

    private fun variant(cardId: String, payload: String) = CardTextEntity(
        cardId = cardId,
        lang = "tr",
        title = "Kart",
        subtitle = null,
        payload = payload,
        updatedAtEpochMillis = Instant.parse("2026-08-24T18:12:00Z").toEpochMilliseconds(),
    )

    private fun card(payload: String) = CardEntity(
        id = Uuid.random().toString(),
        boardId = Uuid.random().toString(),
        title = "Tile",
        subtitle = null,
        icon = "star",
        colorToken = "sand",
        sortIndex = 0,
        visibleToCaregiver = true,
        type = "guide",
        payload = payload,
        locale = null,
        updatedAtEpochMillis = Instant.parse("2026-08-23T18:12:00Z").toEpochMilliseconds(),
    )

    private fun asset(id: Uuid) = MediaAssetEntity(
        id = id.toString(),
        relativePath = "media/$id.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 1_024,
        sha256 = "b".repeat(64),
    )
}

private class FakeCardDao(private val cards: List<CardEntity>) : CardDao {
    override fun observeByBoard(boardId: String): Flow<List<CardEntity>> = flowOf(cards)
    override fun observeAll(): Flow<List<CardEntity>> = flowOf(cards)
    override fun search(query: String): Flow<List<CardEntity>> = flowOf(cards)
    override suspend fun find(id: String): CardEntity? = cards.firstOrNull { it.id == id }
    override suspend fun all(): List<CardEntity> = cards
    override suspend fun upsert(card: CardEntity) = Unit
    override suspend fun updateSortIndex(id: String, sortIndex: Int) = Unit
    override suspend fun delete(id: String) = Unit
}

private class FakeCardTextDao(private val rows: List<CardTextEntity>) : CardTextDao {
    override fun observeForCard(cardId: String): Flow<List<CardTextEntity>> =
        flowOf(rows.filter { it.cardId == cardId })

    override fun observeAll(): Flow<List<CardTextEntity>> = flowOf(rows)
    override suspend fun all(): List<CardTextEntity> = rows
    override suspend fun upsert(text: CardTextEntity) = Unit
    override suspend fun delete(cardId: String, lang: String) = Unit
    override suspend fun deleteForCard(cardId: String) = Unit
}

private class FakeMediaDao(val rows: MutableList<MediaAssetEntity>) : MediaDao {
    override fun observeAll(): Flow<List<MediaAssetEntity>> = flowOf(rows.toList())
    override suspend fun all(): List<MediaAssetEntity> = rows.toList()
    override suspend fun find(id: String): MediaAssetEntity? = rows.firstOrNull { it.id == id }
    override suspend fun upsert(asset: MediaAssetEntity) {
        rows.removeAll { it.id == asset.id }
        rows += asset
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.id == id }
    }
}
