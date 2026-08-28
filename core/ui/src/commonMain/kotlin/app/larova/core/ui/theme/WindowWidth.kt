package app.larova.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * How much width the app has been given, in the three sizes Larova lays out for.
 *
 * One axis, deliberately. Height varies with the keyboard and the system bars and tells a layout
 * almost nothing; width is what decides whether a grid gets a third column and whether a line of
 * a guide is still one the eye can track back to. Branching on the device — "is this a tablet" —
 * would be the wrong question anyway: a phone in landscape, a tablet in split screen and a
 * foldable half-open all have widths that no device category predicts.
 *
 * Material's breakpoints, from [Dimens.MediumWidth] and [Dimens.ExpandedWidth]. Read through
 * [LocalWindowWidth], which [LarovaTheme] fills in from the space it was actually handed — so it
 * follows a window being resized rather than a configuration that was read once.
 */
enum class WindowWidth {
    /** A phone held upright. The layout the design system draws. */
    COMPACT,

    /** A phone on its side, or a small tablet: room for a third tile column, not a fourth. */
    MEDIUM,

    /** A 10" tablet, or a desktop-sized window. Content stops growing and centres instead. */
    EXPANDED,
    ;

    companion object {
        fun of(width: Dp): WindowWidth = when {
            width < Dimens.MediumWidth -> COMPACT
            width < Dimens.ExpandedWidth -> MEDIUM
            else -> EXPANDED
        }
    }
}

/**
 * The active width, anywhere below [LarovaTheme].
 *
 * Defaults to [WindowWidth.COMPACT] rather than to the widest, so a composable rendered outside
 * the theme — a preview, a test that forgot the wrapper — gets the phone layout rather than one
 * that silently assumes room it does not have.
 */
val LocalWindowWidth = staticCompositionLocalOf { WindowWidth.COMPACT }

/**
 * How many tile columns the grid gets.
 *
 * Two on a phone is a design-system decision (`docs/design/design-system.md` §5) and stays exactly
 * that. Three and four are what the same tile size adds up to as the window grows: the tiles keep
 * their proportions instead of stretching, which is the whole reason a wide window gets more of
 * them rather than bigger ones. The start screen and the inside of a folder both use this, because
 * a caregiver who has learned to read one has learned to read the other.
 */
@Composable
fun tileColumns(): Int = when (LocalWindowWidth.current) {
    WindowWidth.COMPACT -> 2
    WindowWidth.MEDIUM -> 3
    WindowWidth.EXPANDED -> 4
}
