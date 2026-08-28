package app.larova.feature.card

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
 * It outlives the *view*, though, which is what lets the same playback move into full screen and
 * back without starting again from the beginning.
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

    // Survives a rotation, because turning the phone is how half of people would try to make a
    // video bigger in the first place.
    var fullscreen by rememberSaveable(absolutePath) { mutableStateOf(false) }

    if (fullscreen) {
        FullscreenPlayback(player = player, onCollapse = { fullscreen = false })
    } else {
        PlayerSurface(
            player = player,
            showVideo = showVideo,
            fullscreen = false,
            onFullscreenChange = { fullscreen = it },
            modifier = modifier,
        )
    }
}

/**
 * The video, filling the screen.
 *
 * A dialog rather than a second destination: full screen is a way of looking at the tile that is
 * already open, not somewhere else to be, and the back gesture should return from it without
 * leaving the tile. The player instance is the one from the tile, so what was playing keeps
 * playing, at the second it had reached.
 *
 * Black behind it, and only for video — a recording has nothing to make bigger, so nothing offers
 * to.
 */
@Composable
private fun FullscreenPlayback(player: ExoPlayer, onCollapse: () -> Unit) {
    Dialog(
        onDismissRequest = onCollapse,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        PlayerSurface(
            player = player,
            showVideo = true,
            fullscreen = true,
            onFullscreenChange = { expanded -> if (!expanded) onCollapse() },
            modifier = Modifier.fillMaxSize(),
            background = Color.Black,
        )
    }
}

/**
 * One `PlayerView`, wherever it is being drawn.
 *
 * `setPlayer(null)` on release matters more than it looks: the player is shared between the tile
 * and the full-screen dialog, and a view that kept its reference on the way out would leave two
 * surfaces claiming one player and a black rectangle in whichever won.
 */
@Composable
private fun PlayerSurface(
    player: ExoPlayer,
    showVideo: Boolean,
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    background: Color? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = true
                // A recording has nothing to look at, so the controls stay put rather than fading
                // out over a black rectangle and leaving somebody tapping to find them again.
                controllerHideOnTouch = showVideo
                controllerShowTimeoutMs = if (showVideo) DEFAULT_TIMEOUT_MILLIS else 0
                // toArgb, not Color.value.toInt(): the latter is the top half of a packed ULong
                // and paints something nobody chose.
                background?.let { setBackgroundColor(it.toArgb()) }

            }
        },
        update = { view ->
            view.player = player
            // Only a video is worth enlarging, and the button appears only when a listener is set —
            // so asking for one is what puts it on the controls. Set here rather than in the
            // factory so it is never the callback from an earlier composition.
            if (showVideo) {
                view.setFullscreenButtonClickListener { expanded -> onFullscreenChange(expanded) }
                view.setFullscreenButtonState(fullscreen)
            }
        },
        onRelease = { view -> view.player = null },
    )
}

/** Media3's own default. Named here because the audio case has to say "not that". */
private const val DEFAULT_TIMEOUT_MILLIS = 5_000
