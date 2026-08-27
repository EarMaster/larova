package app.larova.core.domain.media

import app.larova.core.domain.model.MediaAsset

/**
 * Recording a voice onto a tile.
 *
 * The one piece of content Larova helps make rather than merely holds, and the reason is in
 * `docs/concept.md` §3: a parent reading the bedtime story onto the tile means a caregiver who
 * cannot read the child's language, or cannot read at all, can still play the right words.
 *
 * Stateful on purpose. A recorder is a device that is either running or not, and pretending
 * otherwise — a single `record(duration)` call, say — would mean deciding in advance how long
 * somebody is going to speak.
 *
 * Nothing here throws. A microphone can be taken by a phone call mid-sentence, and losing the
 * recording is the worst that may happen: the tile keeps whatever it had before.
 */
interface AudioRecorder {

    /** True once the microphone is actually running. False if it could not be opened at all. */
    suspend fun start(): Boolean

    /**
     * Stops, and hands back what was recorded.
     *
     * Null when nothing usable came of it — stopped before the encoder had a frame, or interrupted.
     * Whatever was written is cleaned up in that case rather than left as a file no tile points at.
     */
    suspend fun stop(): MediaAsset?

    /**
     * Stops and throws the recording away. What a screen calls when it is left mid-sentence.
     *
     * Deliberately not suspending. It is called from a ViewModel that is being cleared, where the
     * scope to launch anything in is already cancelled — and a microphone that stays open because
     * the coroutine meant to close it was cancelled first is the one leak here a person would
     * actually notice, as a phone that will not record anywhere else until Larova is killed.
     */
    fun cancel()
}
