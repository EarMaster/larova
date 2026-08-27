package app.larova.feature.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Plays one file from app-private storage.
 *
 * `expect` rather than a shared implementation, because playback is the least portable thing this
 * app does: ExoPlayer on Android, AVPlayer on iOS, and nothing shared between them but the path and
 * the question of whether there is a picture to show.
 *
 * [showVideo] rather than two composables. A video and a recording are the same player with the same
 * controls; the only difference is whether a surface is drawn above them, and the caller knows which
 * from the payload it already has.
 *
 * The player is created for this path and released when the screen leaves. A player kept alive
 * behind a screen is the classic way an app keeps a phone awake and a file handle open, which on an
 * app whose whole promise is "it just holds your content" would be an unforced insult.
 */
@Composable
expect fun MediaPlayback(
    absolutePath: String,
    showVideo: Boolean,
    modifier: Modifier = Modifier,
)
