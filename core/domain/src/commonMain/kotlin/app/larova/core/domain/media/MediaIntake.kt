package app.larova.core.domain.media

import app.larova.core.domain.model.MediaAsset

/**
 * Videos and recordings on their way in.
 *
 * Copied byte for byte, unlike a picture. A photograph is re-encoded because 2048px is all a phone
 * screen can show and a camera-sized one only makes the backup larger; a video is left exactly as
 * it is, because transcoding on a phone is slow, lossy, and the one operation in this app that
 * could quietly ruin a recording somebody cannot make again.
 *
 * Copied at all, rather than referenced, for the same reason a picture is: the read permission the
 * picker granted lasts as long as the screen does, and a tile pointing into the gallery goes blank
 * the day somebody tidies it up.
 */
interface MediaIntake {

    /** Copies a picked file in. Null if it cannot be read, or cannot be written. */
    suspend fun copyIn(source: String): MediaAsset?
}

/**
 * What Larova is willing to say about the size of a file.
 *
 * Not a limit. A parent who wants a two-minute video of a bedtime song on a tile gets one; they are
 * simply told what it will do to the backup first, because the export is one file and the person
 * sending it is doing so through whatever their phone offers. `docs/technical-notes.md` §5: videos
 * are not transcoded, but a size threshold triggers a warning.
 */
object MediaSize {

    /** Around a minute of phone video. Past this, a backup stops being a thing you can send. */
    const val WARN_ABOVE_BYTES = 50L * 1024 * 1024
}

/** Whether a file is large enough that somebody should hear about it before it goes on a tile. */
fun isLargeMedia(sizeBytes: Long): Boolean = sizeBytes > MediaSize.WARN_ABOVE_BYTES
