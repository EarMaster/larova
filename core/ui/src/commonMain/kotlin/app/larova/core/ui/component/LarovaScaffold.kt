package app.larova.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.larova.core.ui.icon.BackArrow
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.cd_back
import org.jetbrains.compose.resources.stringResource

/**
 * Every screen in Larova has the same frame: a title, an optional way back, and the help bar.
 *
 * The help bar being part of the frame rather than something each screen adds is the point — it is
 * on every screen because someone who needs it will not be on the screen that happened to include
 * it (docs/concept.md §4.3).
 *
 * Two screens pass `onHelp = null` and have no bar: settings and the activity log. Neither is a
 * screen a caregiver is on while something is going wrong — they are places a parent goes on
 * purpose, and a red emergency bar under a list of preferences dilutes the one colour in the
 * product that is allowed to mean "now". Everything a caregiver actually reads keeps it.
 *
 * The frame is also where the app stops being a phone app. On anything wider than a phone the
 * content is capped at [contentWidth] and centred, and the help bar is capped and centred with it
 * so the two stay one column rather than drifting apart at the edges. Nothing branches on a device
 * type: below 600dp the cap is wider than the screen and none of this has any effect, which is why
 * the phone layout is untouched by it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LarovaScaffold(
    title: String,
    onHelp: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    contentWidth: ContentWidth = ContentWidth.Reading,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = backArrow(),
                                contentDescription = stringResource(Res.string.cd_back),
                            )
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            if (onHelp != null) {
                Centred(contentWidth) { HelpBar(onClick = onHelp) }
            }
        },
        // Above the help bar rather than over it: the bar is the one thing on screen that must
        // never be covered.
        floatingActionButton = floatingActionButton,
        content = { insets -> Centred(contentWidth) { content(insets) } },
    )
}

/**
 * One column, capped and centred.
 *
 * `widthIn` rather than a fixed width: on a phone the cap is never reached, so the child measures
 * exactly as it did before this existed and no golden below 600dp moves by a pixel.
 *
 * Width only, and nothing about height. `Scaffold` measures its bottom bar against the *whole*
 * screen's height and then reserves whatever comes back, so a `fillMaxSize` in here makes the help
 * bar as tall as the window, leaves the content nothing, and draws the bar across the top. The
 * screens fill their own height as they always did.
 */
@Composable
private fun Centred(width: ContentWidth, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = width.max).fillMaxWidth()) { content() }
    }
}

/**
 * Drawn rather than taken from the icon font: the arrow has to mirror in Arabic, and a vector we
 * own is easier to mirror correctly than a glyph whose direction depends on the font.
 */
@Composable
private fun backArrow(): ImageVector = BackArrow
