package app.larova.feature.card

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

/**
 * ExoPlayer behind Media3's own `PlayerView`.
 *
 * The view rather than controls of our own making, on purpose: play, pause and a scrub bar are
 * things every phone owner already knows, and the platform's version of them is the one their
 * muscle memory expects — including the accessibility labelling, which a hand-rolled row of buttons
 * would have to reproduce and would get subtly wrong.
 *
 * Nothing plays by itself. A caregiver who opened a tile to read it should not have a video start
 * talking at them, and in a darkened bedroom that is worse than merely startling.
 *
 * The player is keyed on the path: a different file is a different player, and releasing it when the
 * screen leaves is what stops a file handle and a wake lock outliving the tile that opened them.
 */
@Composable
actual fun MediaPlayback(
    absolutePath: String,
    showVideo: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current

    val player = remember(absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(absolutePath))))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                // A recording has nothing to look at, so the controls stay put rather than fading
                // out over a black rectangle and leaving somebody tapping to find them again.
                controllerHideOnTouch = showVideo
                controllerShowTimeoutMs = if (showVideo) DEFAULT_TIMEOUT_MILLIS else 0
            }
        },
        onRelease = { view -> view.player = null },
    )
}

/** Media3's own default. Named here because the audio case has to say "not that". */
private const val DEFAULT_TIMEOUT_MILLIS = 5_000
