package app.larova.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.help_bar
import app.larova.core.ui.theme.AppMode
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.Signal
import org.jetbrains.compose.resources.stringResource

/**
 * Pinned to the bottom of every screen, in the one colour reserved for it.
 *
 * Alarm red appears exactly once in the product, which is why it is never misread — and why no
 * tile preset is a saturated red. Read under stress, so it says one thing and does one thing.
 */
@Composable
fun HelpBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalAppMode.current != AppMode.LIGHT
    val container = if (dark) Signal.alarmDark else Signal.alarmLight
    val ink = if (dark) Signal.alarmDarkInk else Color.White

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            // The app draws edge to edge, so without this the bar sits under the gesture pill and
            // the bottom of it is somewhere between hard and impossible to hit — on the one control
            // that has to be reachable at a moment nobody is being careful. Horizontal as well as
            // bottom, for the cutout a phone held sideways puts through the middle of it.
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(horizontal = Dimens.HelpBarInset, vertical = Dimens.HelpBarInset)
            .heightIn(min = Dimens.MinTouchTarget),
        shape = RoundedCornerShape(Dimens.TileRadius),
        color = container,
        contentColor = ink,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.help_bar),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
