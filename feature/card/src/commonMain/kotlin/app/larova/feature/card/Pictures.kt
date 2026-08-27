package app.larova.feature.card

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Encoded picture bytes into something Compose can draw.
 *
 * Off the main thread, because decoding a picture the size of a guide step takes long enough to be
 * a dropped frame on the phones this app is meant for. A picture that will not decode comes back
 * null and the step is shown without it — the text is what the guide is for, and a screen that
 * refuses to open because a file went bad would take the words with it.
 */
internal suspend fun ByteArray.toImageBitmapOrNull(): ImageBitmap? =
    withContext(Dispatchers.Default) {
        try {
            decodeToImageBitmap()
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IllegalStateException) {
            null
        }
    }
