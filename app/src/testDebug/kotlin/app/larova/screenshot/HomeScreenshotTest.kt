package app.larova.screenshot

import androidx.compose.runtime.Composable
import app.larova.core.ui.theme.AppMode
import app.larova.feature.home.HomeScreen
import app.larova.feature.home.HomeUiState
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * The start screen, which is the whole app for the person it was built for.
 *
 * Four appearances of the same grid: the three modes, because tile colours are resolved from stored
 * keys against the active one, and 200 % text, because the tile that wraps to three lines at that
 * size is "What helps when he is upset" — the one a caregiver needs most.
 *
 * The states that are not about appearance — an empty grid, a search, parent view — are captured
 * once each in [LightScreensScreenshotTest] rather than four times here. Nothing about them changes
 * with the mode that the grid does not already show.
 *
 * A fifth picture, on a tablet, because this is the screen where width actually changes the layout:
 * four tile columns instead of two, and a search field that stops at reading width instead of
 * running the whole way across.
 */
abstract class HomeScreenshotTest : ScreenshotTest() {

    @Test
    fun grid() {
        capture("home/grid") {
            Home(HomeUiState(tiles = Fixtures.homeTiles, isLoading = false))
        }
    }

    /**
     * Every callback is empty, and that is the point: nothing in a golden may depend on something
     * having happened. A screen is put into the state worth a picture by the state it is handed,
     * never by a tap.
     */
    @Composable
    protected fun Home(state: HomeUiState, isParentView: Boolean = false) {
        HomeScreen(
            state = state,
            isParentView = isParentView,
            onQueryChange = {},
            onClearQuery = {},
            onOpenTile = {},
            onAddTile = {},
            onArrange = {},
            onOpenSettings = {},
            onOpenLog = {},
            onUseTemplate = {},
            onHelp = {},
        )
    }
}

class LightHomeScreenshotTest : HomeScreenshotTest()

class DarkHomeScreenshotTest : HomeScreenshotTest() {
    override val mode = AppMode.DARK
}

class NightHomeScreenshotTest : HomeScreenshotTest() {
    override val mode = AppMode.NIGHT
}

class LargeTextHomeScreenshotTest : HomeScreenshotTest() {
    override val fontScale = LARGE_FONT_SCALE
    override val variant = LARGE_TEXT_VARIANT
}

@Config(qualifiers = TABLET_QUALIFIERS)
class TabletHomeScreenshotTest : HomeScreenshotTest() {
    override val variant = TABLET_VARIANT
}
