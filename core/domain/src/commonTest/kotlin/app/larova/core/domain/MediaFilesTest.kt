package app.larova.core.domain

import app.larova.core.domain.media.MediaIntake
import app.larova.core.domain.media.MediaSize
import app.larova.core.domain.media.isLargeMedia
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.usecase.AddMediaFile
import app.larova.core.domain.usecase.FindMediaFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

/**
 * Videos and recordings, from the moment one is picked to the moment a player asks where it is.
 *
 * The copying, the MIME type and the player belong to the platform and are exercised on a device.
 * What is checked here is what a tile can rely on: that a file is recorded when it arrives, and that
 * a tile whose file is gone says so rather than handing a player a path to nowhere.
 */
@OptIn(ExperimentalUuidApi::class)
class MediaFilesTest {

    @Test
    fun aPickedFileIsCopiedInAndRecorded() = runTest {
        val media = FakeMediaRepository()
        val intake = FakeMediaIntake()

        val asset = AddMediaFile(intake, media)("content://gallery/video/1")

        assertTrue(asset != null)
        assertEquals(asset, media.assets.value.single())
    }

    @Test
    fun aFileThatCannotBeCopiedRecordsNothing() = runTest {
        val media = FakeMediaRepository()

        assertNull(AddMediaFile(FakeMediaIntake(failing = true), media)("content://gone"))
        assertTrue(media.assets.value.isEmpty())
    }

    @Test
    fun aStoredFileIsFoundWithItsTypeAndSize() = runTest {
        val files = FakeMediaFiles()
        val asset = asset("media/song.m4a", mimeType = "audio/mp4", sizeBytes = 42)
        files.putRelative(asset.relativePath, "notes")
        val media = FakeMediaRepository(listOf(asset))

        val found = FindMediaFile(media, files)(asset.id)

        assertEquals(files.absolutePath(asset.relativePath), found?.absolutePath)
        assertEquals("audio/mp4", found?.mimeType)
        assertEquals(42, found?.sizeBytes)
    }

    /**
     * Both halves happen in practice: an import that arrived without its media leaves a row with no
     * file, and a tile deleted from under a screen leaves neither.
     */
    @Test
    fun aRowWithNoFileIsNotAFile() = runTest {
        val asset = asset("media/lost.mp4")
        val media = FakeMediaRepository(listOf(asset))

        // The row is there, the file never arrived.
        assertNull(FindMediaFile(media, FakeMediaFiles())(asset.id))
        // Neither is there.
        assertNull(FindMediaFile(FakeMediaRepository(), FakeMediaFiles())(Uuid.random()))
    }

    @Test
    fun aFileIsCalledLargeOnlyOnceItIs() {
        assertFalse(isLargeMedia(0))
        assertFalse(isLargeMedia(MediaSize.WARN_ABOVE_BYTES))
        assertTrue(isLargeMedia(MediaSize.WARN_ABOVE_BYTES + 1))
    }

    private fun asset(
        relativePath: String,
        mimeType: String = "video/mp4",
        sizeBytes: Long = 1_024,
    ) = MediaAsset(
        id = Uuid.random(),
        relativePath = relativePath,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        sha256 = "hash-of-$relativePath",
    )
}

/** Hands back an asset without a file behind it: what is being checked is the bookkeeping. */
@OptIn(ExperimentalUuidApi::class)
private class FakeMediaIntake(private val failing: Boolean = false) : MediaIntake {

    override suspend fun copyIn(source: String): MediaAsset? {
        if (failing) return null
        val id = Uuid.random()
        return MediaAsset(
            id = id,
            relativePath = "media/$id.mp4",
            mimeType = "video/mp4",
            sizeBytes = source.length.toLong(),
            sha256 = "hash-of-$id",
        )
    }
}
