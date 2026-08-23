package app.larova.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The layout numbers from docs/design/design-system.md §5, in one place so a screen cannot invent
 * its own. `MinTouchTarget` is the one to reach for by default: 56dp rather than Material's 48dp,
 * because the primary reader may be wearing reading glasses and standing up.
 */
object Dimens {
    val ScreenMargin = 20.dp
    val GridGutter = 12.dp
    val MinTouchTarget = 56.dp
    val HelpBarInset = 16.dp
    val TileRadius = 24.dp
    val ChipRadius = 14.dp
    val TileMinHeight = 132.dp
}

val LarovaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(Dimens.ChipRadius),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(Dimens.TileRadius),
    extraLarge = RoundedCornerShape(28.dp),
)
