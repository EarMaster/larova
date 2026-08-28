package app.larova.core.ui.component

import androidx.compose.ui.unit.Dp
import app.larova.core.ui.theme.Dimens

/**
 * How wide a screen's content is allowed to grow before it stops and centres.
 *
 * [LarovaScaffold] owns this rather than each screen, which is what makes the tablet layout one
 * decision instead of twenty: a screen says what kind of thing it is showing, and the width
 * follows. There are only two kinds. Everything that is read or filled in is [Reading]; the two
 * tile grids are [Grid], because a grid is scanned rather than read and has no line of text to
 * lose your place in.
 */
enum class ContentWidth(internal val max: Dp) {
    Reading(Dimens.ReadingWidth),
    Grid(Dimens.GridWidth),
}
