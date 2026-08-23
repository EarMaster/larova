package app.larova

import androidx.compose.runtime.Composable
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.colour_clay
import app.larova.core.ui.resources.colour_lilac
import app.larova.core.ui.resources.colour_moss
import app.larova.core.ui.resources.colour_rose
import app.larova.core.ui.resources.colour_sage
import app.larova.core.ui.resources.colour_sand
import app.larova.core.ui.resources.colour_sky
import app.larova.core.ui.resources.colour_stone
import app.larova.core.ui.theme.TileColor
import app.larova.feature.home.HomeTile
import org.jetbrains.compose.resources.stringResource

/**
 * Eight tiles, one per colour token, until the tile editor lands in M1 and the grid reads from the
 * database.
 *
 * They are the M0 exit criterion made visible. Each carries a token **key**, resolved by the theme
 * per appearance mode, so switching light → dark → night has to change all eight surfaces and all
 * eight accents. A stored hex value would look identical here in light mode and wrong in the other
 * two, which is exactly the failure this milestone exists to make impossible.
 *
 * The labels are the localized colour names, so this also shows that strings resolve out of
 * `:core:ui`'s Compose resources rather than an Android R class.
 */
@Composable
fun sampleTiles(): List<HomeTile> = listOf(
    HomeTile(TileColor.SAND.key, stringResource(Res.string.colour_sand), "☾", TileColor.SAND.key),
    HomeTile(TileColor.CLAY.key, stringResource(Res.string.colour_clay), "✽", TileColor.CLAY.key),
    HomeTile(TileColor.ROSE.key, stringResource(Res.string.colour_rose), "♡", TileColor.ROSE.key),
    HomeTile(TileColor.LILAC.key, stringResource(Res.string.colour_lilac), "☁", TileColor.LILAC.key),
    HomeTile(TileColor.SKY.key, stringResource(Res.string.colour_sky), "☂", TileColor.SKY.key),
    HomeTile(TileColor.SAGE.key, stringResource(Res.string.colour_sage), "✿", TileColor.SAGE.key),
    HomeTile(TileColor.MOSS.key, stringResource(Res.string.colour_moss), "❋", TileColor.MOSS.key),
    HomeTile(TileColor.STONE.key, stringResource(Res.string.colour_stone), "✦", TileColor.STONE.key),
)
