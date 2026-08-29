package app.larova.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.ui.component.HelpBar
import app.larova.core.ui.component.TileCard
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.image
import app.larova.core.ui.theme.AppMode
import app.larova.core.ui.theme.TileColor
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * The colour tokens and the symbol keys, all of them, in one picture per appearance.
 *
 * These are the two tables invariant 1 in `AGENTS.md` freezes: a tile stores `"sage"` and
 * `"moon"`, never a hex value and never a bitmap, and the theme resolves the pair against the
 * active mode. That indirection is the reason a family's tiles stay readable when the phone goes
 * dark — and it is also the reason a mistake in it is invisible, because the light-mode screen
 * everybody looks at is the one place the mapping is hardest to get wrong.
 *
 * Three pictures cover all eight tokens in all three modes. Doing it on the grid instead would
 * need eight tiles per screen and would still miss whichever token nobody happened to use.
 *
 * The alarm red is here too, on the help bar. It is reserved — invariant 4 — and a change to it
 * would otherwise only show on whichever screen somebody happened to re-record.
 */
@Config(qualifiers = "+h800dp")
abstract class TokenScreenshotTest : ScreenshotTest() {

    @Test
    fun colour_tokens() {
        capture("tokens/colours") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(count = 2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(TileColor.entries) { colour ->
                    TileCard(
                        // The English label from the enum, not the localized picker string: this
                        // picture is about the colour, and a translated word in it would make the
                        // golden depend on the locale as well as on the palette.
                        title = colour.label,
                        colorToken = colour.key,
                        symbolKey = TileSymbol.STAR.key,
                        subtitle = colour.key,
                        onClick = {},
                    )
                }
            }
        }
    }

    @Test
    @OptIn(ExperimentalLayoutApi::class)
    fun symbols() {
        capture("tokens/symbols") {
            // A FlowRow rather than a LazyVerticalGrid, and the drawings rather than whole tiles.
            // A lazy grid composes what fits on screen, so the moment the set grew past ten this
            // golden quietly became a picture of the first ten — a check that passes because it
            // stopped looking. Every symbol has to be in the frame for this to be worth having.
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (symbol in TileSymbol.entries) {
                    Column(
                        modifier = Modifier.width(64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = symbol.image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = symbol.key,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    /** Alarm red, which is the help bar and nothing else. */
    @Test
    fun help_bar() {
        capture("tokens/help_bar") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = "Reserved: alarm red, and nowhere but here.")
                HelpBar(onClick = {})
            }
        }
    }
}

class LightTokenScreenshotTest : TokenScreenshotTest()

class DarkTokenScreenshotTest : TokenScreenshotTest() {
    override val mode = AppMode.DARK
}

class NightTokenScreenshotTest : TokenScreenshotTest() {
    override val mode = AppMode.NIGHT
}
