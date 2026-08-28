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
    /**
     * A tile is one height, whatever it says.
     *
     * Content-sized tiles turn a grid into a skyline: two neighbours with a one-line and a two-line
     * title stand at different heights, and eight of them read as clutter rather than as a board.
     * The height is therefore fixed — and split in two, because only one half of a tile grows with
     * the text. [TileChrome] is the padding and the symbol chip, which are dp and stay dp;
     * [TileText] is two lines of title and two of subtitle, which is sp and has to be multiplied by
     * the font scale or a tile clips at 200 %. Use `tileHeight()` rather than adding them by hand.
     */
    val TileChrome = 72.dp
    val TileText = 84.dp

    // ---- Width ----------------------------------------------------------------------------
    //
    // Larova branches on one thing only: how much width there is. Material's two breakpoints,
    // because they are the ones a device actually lands on — a phone in portrait is under 600dp,
    // a phone in landscape and a small tablet sit between, and a 10" tablet is over 840dp. See
    // WindowWidth, which is what screens read; these are the numbers behind it.

    /** Where a phone in portrait stops. Below this, the layout is the one the design system draws. */
    val MediumWidth = 600.dp

    /** Where there is room for a fourth tile column and a genuinely wide window begins. */
    val ExpandedWidth = 840.dp

    /**
     * The widest a column of text, buttons or form fields is allowed to become.
     *
     * A guide step at 22sp spread across 1280dp is a line the eye loses its place in on the way
     * back — the same failure the one-step-per-screen guide exists to prevent. Everything that is
     * read rather than scanned stops here and centres.
     */
    val ReadingWidth = 640.dp

    /**
     * The widest a tile grid is allowed to become.
     *
     * Wider than [ReadingWidth] because a grid is scanned rather than read, and narrower than the
     * screen because four columns of tiles stretched over a 1280dp tablet are four columns of
     * mostly empty card.
     */
    val GridWidth = 1120.dp
}

val LarovaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(Dimens.ChipRadius),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(Dimens.TileRadius),
    extraLarge = RoundedCornerShape(28.dp),
)
