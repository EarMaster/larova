package app.larova.core.data

import app.larova.core.data.db.CardDao
import app.larova.core.data.db.CardEntity
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

        val removed = RoomMediaRepository(media, FakeCardDao(cards)).deleteOrphans()

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

            val removed = RoomMediaRepository(media, FakeCardDao(cards)).deleteOrphans()

            assertEquals(0, removed)
            assertEquals(2, media.rows.size)
        }
    }

    @Test
    fun anEmptyLibraryIsNotAnError() = runTest {
        val media = FakeMediaDao(mutableListOf())
        assertEquals(0, RoomMediaRepository(media, FakeCardDao(emptyList())).deleteOrphans())
        assertTrue(media.rows.isEmpty())
    }

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
    override fun search(query: String): Flow<List<CardEntity>> = flowOf(cards)
    override suspend fun find(id: String): CardEntity? = cards.firstOrNull { it.id == id }
    override suspend fun all(): List<CardEntity> = cards
    override suspend fun upsert(card: CardEntity) = Unit
    override suspend fun updateSortIndex(id: String, sortIndex: Int) = Unit
    override suspend fun delete(id: String) = Unit
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
