package app.larova.screenshot

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.larova.core.ui.theme.AppMode
import app.larova.core.ui.theme.LarovaTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base for the screenshot tests: renders the real UI on the JVM and writes it to a PNG.
 *
 * Robolectric with native graphics rather than an emulator, for the same reason the goldens are
 * worth having at all — they have to be the same picture every run. An emulator brings a real
 * clock, a real font cache and a real compositor, and a golden that differs by a pixel because a
 * machine was busy is a check nobody reads twice.
 *
 * Larova's screens all take their state as a parameter and hand their events back out, so there is
 * no dependency injection anywhere in here: a picture is made from a fixture, not from a database.
 * That is why these tests need no Koin, no Room and no DataStore, and why adding a screen to the
 * set is a fixture and three lines rather than a fake repository.
 *
 * Nothing here writes a file during `./gradlew test`. [captureRoboImage] is inert unless Roborazzi's
 * own tasks turn it on, so an ordinary test run only checks that every screen still composes —
 * which is worth having on its own, since a screen that throws on an empty list would otherwise
 * only be found by opening it.
 *
 * **One capture per test method.** The compose rule refuses a second `setContent` on the same
 * activity, so a screen photographed in four appearances is four runs of the same method on four
 * subclasses — see [mode] and [fontScale] — and never a loop inside one.
 */
@RunWith(AndroidJUnit4::class)
abstract class ScreenshotTest {

    // The v1 rule, deliberately, and it warns about it. The v2 replacement swaps the unconfined
    // test dispatcher for a standard one, which queues work instead of running it immediately —
    // and what these tests depend on running immediately is the Compose Multiplatform resource
    // reader that fills in every string. Moving to v2 means finding out what each golden then
    // needs synchronising, which is a change to make on purpose rather than in passing.
    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * The appearance this class captures in.
     *
     * Passed to the theme explicitly rather than left to the system setting, because night is the
     * whole reason this matrix exists: tile colours are resolved from stored keys against the
     * active mode (invariant 1 in `AGENTS.md`), and a token that resolves wrong there is invisible
     * in the light-mode picture everybody looks at.
     */
    protected open val mode: AppMode = AppMode.LIGHT

    /**
     * The text scale this class captures at.
     *
     * 200 % is the accessibility floor `AGENTS.md` sets, and clipping at that size is a layout bug
     * that no amount of reading the code finds.
     */
    protected open val fontScale: Float = 1f

    /** What a golden's file name ends in: `light`, `dark`, `night`, `large_text`. */
    protected open val variant: String get() = mode.fileSuffix

    /** Renders [content] and writes `src/testDebug/screenshots/[name]_[variant].png`. */
    protected fun capture(name: String, content: @Composable () -> Unit) {
        show(content = content)
        captureRoot("${name}_$variant")
    }

    /**
     * Composes without capturing, for UI that has to be *put* into the state worth a picture — a
     * dialog opened, a field typed into. Pair it with [captureRoot] or [captureScreen].
     */
    protected fun show(appearance: AppMode = mode, content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
            ) {
                LarovaTheme(mode = appearance) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        content()
                    }
                }
            }
        }
        // Compose Multiplatform loads strings through a resource reader that finishes after the
        // first frame, so a capture taken without this is a picture of the layout with every label
        // still empty. Idling is enough — the reader posts back to the main looper.
        compose.waitForIdle()
    }

    protected fun captureRoot(fileName: String) {
        captureTo("$GOLDEN_DIR/$fileName.png")
    }

    /**
     * Captures every window at once, for UI that opens one of its own.
     *
     * An `AlertDialog` composes into a separate window rather than into the activity's, so
     * [captureRoot] fails outright there — `onRoot()` matches two roots and cannot choose. This
     * composites the lot, which is also the only way to get the dialog *and* the screen behind it
     * into one picture, scrim included.
     */
    @OptIn(ExperimentalRoborazziApi::class)
    protected fun captureScreen(fileName: String) {
        captureScreenRoboImage("$GOLDEN_DIR/$fileName.png")
    }

    /**
     * Writes the window to an arbitrary path, relative to the `:app` module directory.
     *
     * Used by the store and website images, which are products rather than baselines and therefore
     * land outside the golden directory — under `fastlane/` and `docs/pages/`.
     */
    protected fun captureTo(path: String) {
        compose.onRoot().captureRoboImage(path)
    }

    private companion object {
        /** Committed next to the tests, so a review sees the picture beside the change. */
        const val GOLDEN_DIR = "src/testDebug/screenshots"
    }
}

/** `light`, `dark`, `night` — what a golden's file name carries. */
internal val AppMode.fileSuffix: String get() = name.lowercase()

/** The accessibility floor Larova promises: everything readable, nothing clipped, at 200 %. */
internal const val LARGE_FONT_SCALE = 2f

/** What the 200 % goldens are called, so the string is written once. */
internal const val LARGE_TEXT_VARIANT = "large_text"

/** What the tablet goldens are called. */
internal const val TABLET_VARIANT = "tablet"

/**
 * A 10" tablet on its side: 1280×800 dp at 240 dpi, so 1920×1200 px.
 *
 * Landscape rather than portrait, because landscape is the hard case and the one a tablet is
 * usually held in. It is the width that decides Larova's layout — four tile columns above 840dp,
 * everything that is read capped and centred (`WindowWidth`) — and 1280dp is the widest a screen
 * in this product realistically gets. A portrait tablet at 800dp lands in the middle band and is
 * covered by the same code path with one column fewer.
 *
 * Replaces `robolectric.properties` outright rather than narrowing it with `+`, because every
 * qualifier in it is wrong for a tablet: the size bucket, the aspect and the density all change
 * together.
 */
internal const val TABLET_QUALIFIERS =
    "w1280dp-h800dp-xlarge-notlong-notround-any-240dpi-keyshidden-nonav"
