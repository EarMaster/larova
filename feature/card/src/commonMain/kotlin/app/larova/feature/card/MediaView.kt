package app.larova.feature.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.media_missing
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.GuideStepStyle
import org.jetbrains.compose.resources.stringResource

/**
 * A video or a recording, with whatever the parents wrote above it.
 *
 * The caption comes first and at reading size. On a video tile it is often the whole point — "this
 * is how he likes his hair done" — and on a recording it is the only thing on screen until somebody
 * presses play.
 *
 * [absolutePath] null means the row is there and the file is not: an import that arrived without
 * its media, or a phone that ran out of space during one. Said in words rather than shown as a
 * player that fails when it is tapped.
 */
@Composable
fun MediaView(
    caption: String?,
    absolutePath: String?,
    showVideo: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = GuideStepStyle,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (absolutePath == null) {
            Text(
                text = stringResource(Res.string.media_missing),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        MediaPlayback(
            absolutePath = absolutePath,
            showVideo = showVideo,
            modifier = Modifier
                .fillMaxWidth()
                // Bounded so a portrait video cannot push its own controls off the bottom of the
                // screen, and so a recording is a strip rather than a black wall.
                .sizeIn(maxHeight = if (showVideo) 360.dp else 96.dp),
        )
    }
}
