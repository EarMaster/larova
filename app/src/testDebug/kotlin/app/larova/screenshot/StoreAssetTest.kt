package app.larova.screenshot

import androidx.compose.runtime.Composable
import app.larova.core.ui.theme.AppMode
import app.larova.feature.card.CardScreen
import app.larova.feature.card.CardUiState
import app.larova.feature.help.HelpScreen
import app.larova.feature.home.HomeScreen
import app.larova.feature.home.HomeUiState
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * The Play Store listing images and the website's screenshots, from the same harness as the goldens
 * but with a different purpose — and therefore different rules.
 *
 * These are **products, not baselines**: they are regenerated deliberately and compared against
 * nothing, so they are excluded from the verify job by a `--tests` filter. A UI change should fail
 * one job, not two.
 *
 *     ./gradlew :app:recordRoborazziDebug --tests '*StoreAssetTest'
 *
 * Play's rules dictate the shape of what follows:
 *
 * - **24-bit PNG, no alpha.** Roborazzi writes RGBA, so every file is converted on the way out by
 *   the `finishStoreAssets` Gradle task. An image with an alpha channel is refused at upload.
 * - **The longest side may be at most twice the shortest**, and every side between 320 and 3840 px.
 *   The goldens' 1078×2399 frame is 2.23:1 and would be rejected outright, which is the whole
 *   reason these are generated separately rather than copied across.
 * - Play recommends 1080 px on the short side; 411 dp at xxhdpi is 1233, comfortably over.
 *
 * Two frames, because Play has two slots and Larova has two layouts: a phone in portrait and a 10"
 * tablet on its side. See [EnglishTabletStoreAssetTest] for why the tablet set is generated rather
 * than left to Play to stretch.
 *
 * `tools/check_store_metadata.sh` enforces all of that on the files themselves, so a hand-added
 * image is checked exactly as a generated one is.
 *
 * **A locale needs two halves, and the qualifier is the easy one.** The app's own chrome follows
 * the `@Config` locale; the tile titles, guide steps and names do not, because they are *family
 * content* and come from [StoreContent]. A set generated with only the first half is a listing in
 * one language showing an app apparently set up in another. The app itself now speaks all fourteen
 * languages, so the qualifier half is available everywhere; what is missing for the other twelve is
 * a [StoreContent] — the tile titles, guide steps and names a family in that language would have
 * written. Play falls back to the default locale's images wherever a locale has none, so those
 * twelve show the English set until somebody writes one. See `AGENTS.md`, "Store assets".
 */
abstract class StoreAssetTest : ScreenshotTest() {

    /** The Play locale directory these land in — `en-US`, `de-DE`. Not the app's language tag. */
    protected abstract val storeLocale: String

    /** The family this locale's pictures show. */
    protected abstract val content: StoreContent

    /**
     * Which of Play's image sets these belong in.
     *
     * `phoneScreenshots` and `tenInchScreenshots` are folder names `fastlane supply` reads, not
     * labels — a typo here is a folder Play never sees. The tablet set is worth generating rather
     * than letting Play stretch the phone one: Larova genuinely lays out differently above 840dp,
     * and a listing that shows a two-column grid on a tablet is advertising a layout the app does
     * not have. `check_store_metadata.sh` validates whatever sets exist, so nothing else needs to
     * know this list.
     */
    protected open val imageSet: String = "phoneScreenshots"

    /** The start screen: eight tiles, the search field, the help bar. What the app *is*. */
    @Test
    fun start_screen() {
        show { home() }
        store("01_start")
    }

    /**
     * A guide open at reading size.
     *
     * The second image on the listing rather than the fifth, because "one step at a time, large
     * enough to read across a room" is the thing a shopper cannot infer from the grid.
     */
    @Test
    fun guide() {
        show { card(content.guide) }
        store("02_guide")
    }

    /** Something a caregiver does rather than reads: ticking off, which needs no unlocking. */
    @Test
    fun checklist() {
        show { card(content.checklist) }
        store("03_checklist")
    }

    /** The numbers behind the red bar, which is the feature the listing text leads with. */
    @Test
    fun help_sheet() {
        show {
            HelpScreen(
                contacts = content.helpContacts,
                onPrepareCall = {},
                onBack = {},
                onHelp = {},
            )
        }
        store("04_help")
    }

    /**
     * The same start screen at night.
     *
     * Last, and worth the slot: night mode is not a third theme for its own sake but the one that
     * makes reading a bedtime guide aloud in a dark room possible, and a picture says that faster
     * than the description can.
     */
    @Test
    fun night_mode() {
        show(appearance = AppMode.NIGHT) { home() }
        store("05_night")
    }

    @Composable
    private fun home() {
        HomeScreen(
            state = HomeUiState(tiles = content.homeTiles, isLoading = false),
            isParentView = false,
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

    @Composable
    private fun card(state: CardUiState) {
        CardScreen(
            state = state,
            isParentView = false,
            onToggleItem = {},
            onPrepareCall = {},
            onOpenUrl = {},
            onOpenApp = {},
            onTranslate = {},
            onEdit = {},
            onBack = {},
            onHelp = {},
        )
    }

    /**
     * Captures straight into the fastlane layout.
     *
     * The numeric prefix is the order Play and fastlane list them in, which is filename order — so
     * the numbers are the running order of the listing, not decoration.
     */
    private fun store(name: String) {
        captureTo("$STORE_DIR/$storeLocale/images/$imageSet/$name.png")
    }

    private companion object {
        /** Relative to the `:app` module directory, which is where a test's paths resolve. */
        const val STORE_DIR = "../fastlane/metadata/android"
    }
}

/**
 * A phone at 411×820 dp, xxhdpi: 1233×2460.
 *
 * As tall as Play allows and no taller — 1.995:1, just inside the rule that the long side may not
 * exceed twice the short one. Real phones are taller than that (the goldens' frame is 2.23:1), and
 * the dp given back are not free: every 20 dp is 20 dp of a guide that fits without scrolling.
 *
 * The locale belongs in this string too, in Android's own qualifier order — before the size. Set
 * from inside a test body it silently does nothing: the activity has already resolved its
 * configuration by then, and the picture comes out in English whatever the body asked for.
 */
@Config(qualifiers = "en-rUS-w411dp-h820dp-normal-long-notround-any-xxhdpi-keyshidden-nonav")
class EnglishStoreAssetTest : StoreAssetTest() {
    override val storeLocale = "en-US"
    override val content = EnglishStoreContent
}

@Config(qualifiers = "de-rDE-w411dp-h820dp-normal-long-notround-any-xxhdpi-keyshidden-nonav")
class GermanStoreAssetTest : StoreAssetTest() {
    override val storeLocale = "de-DE"
    override val content = GermanStoreContent
}

/**
 * The same five screens on a 10" tablet in landscape: 1280×800 dp at xhdpi, so 2560×1600.
 *
 * A second frame rather than a second crop of the first. Play has its own upload slot for tablet
 * screenshots and shows it to somebody shopping on a tablet, and Larova is not the same layout
 * there — four tile columns instead of two, and everything that is read held to a column in the
 * middle instead of run to the edges. Stretching the phone set to fill it would be the one thing a
 * listing screenshot must not be, which is a picture of an app that does not exist.
 *
 * 1.6:1, comfortably inside Play's rule that the long side may not exceed twice the short one —
 * the constraint that dictates the phone frame is not one a tablet comes anywhere near.
 */
@Config(qualifiers = "en-rUS-w1280dp-h800dp-xlarge-notlong-notround-any-xhdpi-keyshidden-nonav")
class EnglishTabletStoreAssetTest : StoreAssetTest() {
    override val storeLocale = "en-US"
    override val content = EnglishStoreContent
    override val imageSet = "tenInchScreenshots"
}

@Config(qualifiers = "de-rDE-w1280dp-h800dp-xlarge-notlong-notround-any-xhdpi-keyshidden-nonav")
class GermanTabletStoreAssetTest : StoreAssetTest() {
    override val storeLocale = "de-DE"
    override val content = GermanStoreContent
    override val imageSet = "tenInchScreenshots"
}
