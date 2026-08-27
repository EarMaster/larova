package app.larova.core.platform

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import app.larova.core.domain.export.MediaFiles
import app.larova.core.domain.media.AudioRecorder
import app.larova.core.domain.model.MediaAsset
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * `MediaRecorder`, writing AAC into an MPEG-4 container — the format every Android phone can both
 * record and play, and one that needs no library on the playback side either.
 *
 * Straight into `filesDir/media` rather than into a temporary file that is copied afterwards. A
 * recording can be minutes long, and copying it once it exists is a second full write on a phone
 * that may be low on space for a file that is already where it belongs.
 *
 * Two things here are less obvious than they look:
 *
 * The recorder is a device, so this class holds the one that is running and refuses to start a
 * second. Two recorders on one microphone is not an error the framework reports usefully — it
 * simply fails, and the parent gets a tile with silence on it.
 *
 * A recording that produced nothing takes its file with it. `MediaRecorder.stop` throws when it
 * has not encoded a frame yet, which is exactly what happens when somebody taps record and stop in
 * the same second, and the file it leaves is a valid container with no audio in it.
 */
@OptIn(ExperimentalUuidApi::class)
class AndroidAudioRecorder(
    private val context: Context,
    private val mediaFiles: MediaFiles,
) : AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var target: Recording? = null

    override suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        if (recorder != null) return@withContext false

        val id = Uuid.random()
        val relativePath = PlatformNames.MEDIA_DIRECTORY + "/" + id + "." + EXTENSION
        val file = File(mediaFiles.absolutePath(relativePath))

        val started = try {
            newRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(BIT_RATE)
                setAudioSamplingRate(SAMPLE_RATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
                recorder = this
            }
            true
        } catch (_: IOException) {
            false
        } catch (_: IllegalStateException) {
            false
        } catch (_: RuntimeException) {
            // What the framework throws when the microphone is not available — held by a call, or
            // taken by another app. Nothing the person could do about it, and nothing to crash for.
            false
        }

        if (started) {
            target = Recording(id = id, relativePath = relativePath, file = file)
        } else {
            release()
            file.delete()
        }
        started
    }

    override suspend fun stop(): MediaAsset? = withContext(Dispatchers.IO) {
        val running = recorder ?: return@withContext null
        val recording = target

        val stopped = try {
            running.stop()
            true
        } catch (_: IllegalStateException) {
            // Stopped before a single frame was encoded. The file exists and holds nothing.
            false
        } catch (_: RuntimeException) {
            false
        }
        release()

        if (recording == null || !stopped || !recording.holdsAudio()) {
            recording?.file?.delete()
            return@withContext null
        }

        MediaAsset(
            id = recording.id,
            relativePath = recording.relativePath,
            mimeType = MIME_TYPE,
            sizeBytes = recording.file.length(),
            sha256 = recording.file.sha256(),
        )
    }

    /**
     * On whatever thread called it, because the thing that calls it is a ViewModel being cleared.
     * `stop` and `release` are a handful of milliseconds and the alternative is a microphone that
     * outlives the screen.
     */
    override fun cancel() {
        val running = recorder
        if (running != null) {
            try {
                running.stop()
            } catch (_: IllegalStateException) {
                // Never got going. Nothing to stop, and the file goes either way.
            } catch (_: RuntimeException) {
                // As above.
            }
        }
        release()
        target?.file?.delete()
        target = null
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            // Deprecated from API 31, and the only constructor below it. minSdk is 26.
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun release() {
        recorder?.release()
        recorder = null
    }

    /** What is being written, so that stopping knows which file to look at. */
    private data class Recording(val id: Uuid, val relativePath: String, val file: File) {

        /**
         * A container with no audio in it is what a record-then-stop in the same second leaves
         * behind, and it is a file that plays as silence rather than failing to open.
         */
        fun holdsAudio(): Boolean = file.isFile && file.length() > 0L
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte ->
            byte.toInt().and(BYTE_MASK).toString(RADIX_HEX).padStart(HEX_DIGITS, '0')
        }
    }

    private companion object {
        const val EXTENSION = "m4a"
        const val MIME_TYPE = "audio/mp4"

        /** Speech, not music: plenty for a voice and small enough to sit in a backup. */
        const val BIT_RATE = 96_000
        const val SAMPLE_RATE = 44_100

        const val BUFFER_BYTES = 8 * 1024
        const val RADIX_HEX = 16
        const val BYTE_MASK = 0xFF
        const val HEX_DIGITS = 2
    }
}
