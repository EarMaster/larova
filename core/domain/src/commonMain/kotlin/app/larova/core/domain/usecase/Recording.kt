package app.larova.core.domain.usecase

import app.larova.core.domain.media.AudioRecorder
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.repository.MediaRepository

/**
 * Recording a voice, and writing down what came of it.
 *
 * The recorder produces the file; this is what makes it part of the app's content. Registered on
 * stop rather than on start, so a recording that was cancelled — or that the microphone lost — leaves
 * no row behind pointing at nothing.
 */
class Recording(
    private val recorder: AudioRecorder,
    private val media: MediaRepository,
) {

    suspend fun start(): Boolean = recorder.start()

    suspend fun stop(): MediaAsset? {
        val asset = recorder.stop() ?: return null
        media.register(asset)
        return asset
    }

    /**
     * Called when the editor goes away mid-recording.
     *
     * Silent by design: somebody who has left the screen is not waiting to be told that the
     * recording they abandoned was abandoned.
     */
    fun cancel() = recorder.cancel()
}
