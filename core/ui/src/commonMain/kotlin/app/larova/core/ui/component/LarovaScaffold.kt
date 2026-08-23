package app.larova.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LarovaScaffold(
    title: String,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
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
        bottomBar = { HelpBar(onClick = onHelp) },
        content = content,
    )
}

/**
 * Drawn rather than taken from the icon font: the arrow has to mirror in Arabic, and a vector we
 * own is easier to mirror correctly than a glyph whose direction depends on the font.
 */
@Composable
private fun backArrow(): ImageVector = BackArrow
