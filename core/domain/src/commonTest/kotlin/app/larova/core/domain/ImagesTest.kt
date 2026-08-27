package app.larova.core.domain

import app.larova.core.domain.media.ImageSize
import app.larova.core.domain.media.ImageStore
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.usecase.AddImage
import app.larova.core.domain.usecase.CleanUpMedia
import app.larova.core.domain.usecase.LoadImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * Pictures on a guide step, from the moment one is picked to the moment nothing points at it.
 *
 * The decoding, the downscaling and the file itself are the platform's and are exercised on a
 * device. What is checked here is the part that decides whether a picture is still there next
 * week: that picking one records it, that a picking that failed records nothing, and that a picture
 * taken off a step takes its file with it — but only once no step wants it.
 */
@OptIn(ExperimentalUuidApi::class)
class ImagesTest {

    @Test
    fun aPickedPictureIsStoredAndRecorded() = runTest {
        val images = FakeImageStore()
        val media = FakeMediaRepository()

        val id = AddImage(images, media)("content://gallery/1")

        assertTrue(id != null)
        val asset = media.assets.value.single()
        assertEquals(id, asset.id)
        assertEquals("image/jpeg", asset.mimeType)
    }

    @Test
    fun aPictureThatCannotBeReadRecordsNothing() = runTest {
        val images = FakeImageStore(failStores = true)
        val media = FakeMediaRepository()

        assertNull(AddImage(images, media)("content://gallery/gone"))
        assertTrue(media.assets.value.isEmpty())
    }

    @Test
    fun aPictureIsReadAtTheSizeTheScreenAsksFor() = runTest {
        val images = FakeImageStore()
        val media = FakeMediaRepository()
        val id = AddImage(images, media)("content://gallery/1")

        val bytes = LoadImage(images, media)(requireNotNull(id), ImageSize.THUMBNAIL)

        assertTrue(bytes != null)
        assertEquals(ImageSize.THUMBNAIL, images.lastRequestedEdge)
    }

    /** A row that is gone is a picture that is gone, whatever is still lying on the disk. */
    @Test
    fun aPictureWithNoRowIsNotRead() = runTest {
        val images = FakeImageStore()
        val media = FakeMediaRepository()

        assertNull(LoadImage(images, media)(Uuid.random(), ImageSize.ON_SCREEN))
    }

    @Test
    fun theSweepTakesTheFileOfAPictureNoStepWants() = runTest {
        val files = FakeMediaFiles()
        val kept = asset("media/kept.jpg")
        val dropped = asset("media/dropped.jpg")
        files.putRelative(kept.relativePath, "a picture")
        files.putRelative(dropped.relativePath, "another picture")
        val media = FakeMediaRepository(listOf(kept, dropped)).apply { orphans = setOf(dropped.id) }

        val removed = CleanUpMedia(media, files)()

        assertEquals(1, removed)
        assertEquals(listOf(kept), media.observeAll().first())
        assertEquals("a picture", files.contentOf(kept.relativePath))
        assertNull(files.contentOf(dropped.relativePath))
    }

    @Test
    fun theSweepLeavesAPictureThatIsStillOnAStep() = runTest {
        val files = FakeMediaFiles()
        val kept = asset("media/kept.jpg")
        files.putRelative(kept.relativePath, "a picture")
        val media = FakeMediaRepository(listOf(kept))

        assertEquals(0, CleanUpMedia(media, files)())
        assertEquals("a picture", files.contentOf(kept.relativePath))
    }

    private fun asset(relativePath: String) = MediaAsset(
        id = Uuid.random(),
        relativePath = relativePath,
        mimeType = "image/jpeg",
        sizeBytes = 9L,
        sha256 = "hash-of-$relativePath",
    )
}

/**
 * A picture store with no pictures in it.
 *
 * It records what it was asked for rather than producing anything image-shaped: what these tests
 * are about is which calls happen and what is written down afterwards, and a real JPEG here would
 * only make that harder to read.
 */
@OptIn(ExperimentalUuidApi::class)
private class FakeImageStore(private val failStores: Boolean = false) : ImageStore {

    var lastRequestedEdge: Int? = null
        private set

    private val stored = mutableMapOf<String, ByteArray>()

    override suspend fun store(source: String): MediaAsset? {
        if (failStores) return null
        val id = Uuid.random()
        val relativePath = "media/$id.jpg"
        stored[relativePath] = source.encodeToByteArray()
        return MediaAsset(
            id = id,
            relativePath = relativePath,
            mimeType = "image/jpeg",
            sizeBytes = stored.getValue(relativePath).size.toLong(),
            sha256 = "hash-of-$id",
        )
    }

    override suspend fun read(relativePath: String, maxEdge: Int): ByteArray? {
        lastRequestedEdge = maxEdge
        return stored[relativePath]
    }
}
