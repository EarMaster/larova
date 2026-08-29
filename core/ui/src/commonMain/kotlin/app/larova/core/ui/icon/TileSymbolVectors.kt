package app.larova.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The vector for a symbol key, or null if this version has never heard of it.
 *
 * Null is a real answer rather than a failure. A tile written by a newer Larova can carry a key
 * this build has no drawing for, and [TileSymbol.fromKey] turns that into the default symbol — the
 * tile still opens, still says what the parents titled it, and only the picture is missing. The
 * alternative, refusing to draw the screen, would take a family's whole board away over an icon.
 *
 * Built once per key and kept. Sixty-eight `ImageVector`s is not much memory, and the start screen
 * is the one screen that has to appear immediately; rebuilding a symbol on every recomposition of
 * a scrolling grid is exactly the work that shows up as a stutter.
 */
internal fun tileSymbolVector(key: String): ImageVector? {
    val paths = SYMBOL_PATHS[key] ?: return null
    return cache.getOrPut(key) { buildSymbol(key, paths) }
}

private val cache = mutableMapOf<String, ImageVector>()

/**
 * 24×24, 2px, round caps and joins — the geometry every drawing in `core/ui/icons/` is built on,
 * vendored and hand-drawn alike. Stated once here rather than in sixty-eight files, which is what
 * keeps a symbol somebody adds later from arriving a quarter-pixel heavier than its neighbours.
 */
private fun buildSymbol(name: String, paths: List<SymbolPath>): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = ICON_SIZE.dp,
        defaultHeight = ICON_SIZE.dp,
        viewportWidth = ICON_SIZE,
        viewportHeight = ICON_SIZE,
    ).apply {
        for (path in paths) {
            val nodes = PathParser().parsePathString(path.data).toNodes()
            if (path.filled) {
                // A dot rather than a line — an eye, a pip on a die. Filled shapes carry no
                // stroke, or they come out a third larger than they were drawn.
                addPath(pathData = nodes, fill = SolidColor(Color.Black))
            } else {
                addPath(
                    pathData = nodes,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = STROKE,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
    }.build()

/**
 * The last resort, if even the default symbol has no drawing.
 *
 * An empty 24dp square rather than a crash or a question mark: a tile whose picture went missing
 * should still open, still be the colour the parents chose, and still say what they titled it.
 */
internal val BlankSymbol: ImageVector by lazy {
    ImageVector.Builder(
        name = "Blank",
        defaultWidth = ICON_SIZE.dp,
        defaultHeight = ICON_SIZE.dp,
        viewportWidth = ICON_SIZE,
        viewportHeight = ICON_SIZE,
    ).build()
}

private const val ICON_SIZE = 24f
private const val STROKE = 2f
