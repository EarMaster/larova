package app.larova.core.domain

import app.larova.core.domain.media.AudioRecorder
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.usecase.Recording
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

/**
 * Recording a voice onto a tile — the one piece of content Larova helps make rather than holds.
 *
 * The microphone belongs to the platform and is exercised on a device. What is checked here is the
 * bookkeeping around it, which is where a recording gets lost: a row written for a recording that
 * never happened is a tile that plays nothing, and no row for one that did is a file nobody can
 * reach.
 */
@OptIn(ExperimentalUuidApi::class)
class RecordingTest {

    @Test
    fun aFinishedRecordingIsRecorded() = runTest {
        val media = FakeMediaRepository()
        val recorder = FakeAudioRecorder()
        val recording = Recording(recorder, media)

        assertTrue(recording.start())
        val asset = recording.stop()

        assertEquals(asset, media.assets.value.single())
    }

    /** Nothing is written down until there is something to write down about. */
    @Test
    fun startingAloneWritesNothing() = runTest {
        val media = FakeMediaRepository()
        val recording = Recording(FakeAudioRecorder(), media)

        recording.start()

        assertTrue(media.assets.value.isEmpty())
    }

    @Test
    fun aMicrophoneThatCannotBeOpenedIsReported() = runTest {
        val media = FakeMediaRepository()
        val recording = Recording(FakeAudioRecorder(failing = true), media)

        assertFalse(recording.start())
        assertNull(recording.stop())
        assertTrue(media.assets.value.isEmpty())
    }

    /**
     * Record and stop in the same second: the recorder has nothing usable, so the tile keeps
     * whatever it had before rather than being pointed at silence.
     */
    @Test
    fun aRecordingThatProducedNothingLeavesNoRow() = runTest {
        val media = FakeMediaRepository()
        val recorder = FakeAudioRecorder(producesNothing = true)
        val recording = Recording(recorder, media)

        recording.start()

        assertNull(recording.stop())
        assertTrue(media.assets.value.isEmpty())
    }

    @Test
    fun cancellingWritesNothingAndReleasesTheDevice() = runTest {
        val media = FakeMediaRepository()
        val recorder = FakeAudioRecorder()
        val recording = Recording(recorder, media)

        recording.start()
        recording.cancel()

        assertTrue(media.assets.value.isEmpty())
        assertTrue(recorder.cancelled)
    }
}

@OptIn(ExperimentalUuidApi::class)
private class FakeAudioRecorder(
    private val failing: Boolean = false,
    private val producesNothing: Boolean = false,
) : AudioRecorder {

    var cancelled = false
        private set

    private var running = false

    override suspend fun start(): Boolean {
        if (failing) return false
        running = true
        return true
    }

    override suspend fun stop(): MediaAsset? {
        if (!running || producesNothing) return null
        running = false
        val id = Uuid.random()
        return MediaAsset(
            id = id,
            relativePath = "media/$id.m4a",
            mimeType = "audio/mp4",
            sizeBytes = 2_048,
            sha256 = "hash-of-$id",
        )
    }

    override fun cancel() {
        running = false
        cancelled = true
    }
}
