package app.larova.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The symbols a tile can carry.
 *
 * [key] is what `Card.icon` stores and what every export file contains, so these strings are
 * frozen exactly as the colour tokens are. Adding a key later is safe; renaming or removing one
 * means migrating other people's data, including data that exists only in a backup nobody can
 * reach.
 *
 * The drawing is deliberately on this side of the boundary. Storing a bitmap — or a font glyph,
 * which is a bitmap someone else chose — would tie a family's tiles to a rendering that cannot be
 * corrected. A key can be redrawn a hundred times and their tiles keep meaning what they meant.
 */
enum class TileSymbol(val key: String) {
    MOON("moon"),
    SUN("sun"),
    HEART("heart"),
    LIST("list"),
    NOTE("note"),
    PHONE("phone"),
    CLOCK("clock"),
    HOME("home"),
    MEAL("meal"),
    STAR("star"),
    ;

    companion object {
        val DEFAULT = STAR

        /**
         * Unknown keys resolve to the default rather than failing, which is what lets a tile
         * written by a newer version still draw. The tile keeps its stored key: the fallback is a
         * rendering decision, not an edit.
         */
        fun fromKey(key: String?): TileSymbol = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** The vector for this symbol. One per key, drawn in a 24×24 viewport. */
val TileSymbol.image: ImageVector
    get() = when (this) {
        TileSymbol.MOON -> Moon
        TileSymbol.SUN -> Sun
        TileSymbol.HEART -> Heart
        TileSymbol.LIST -> ListLines
        TileSymbol.NOTE -> NoteSheet
        TileSymbol.PHONE -> Phone
        TileSymbol.CLOCK -> Clock
        TileSymbol.HOME -> House
        TileSymbol.MEAL -> Bowl
        TileSymbol.STAR -> Star
    }

private const val VIEWPORT = 24f
private const val STROKE = 1.8f

private fun symbol(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    ).apply(block).build()

/** A crescent as one circle minus another, so it stays a crescent at any size. */
private val Moon: ImageVector by lazy {
    symbol("Moon") {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(12f, 3f)
            arcToRelative(9f, 9f, 0f, true, false, 0f, 18f)
            arcToRelative(9f, 9f, 0f, true, false, 0f, -18f)
            close()
            moveTo(16f, 3.5f)
            arcToRelative(8.5f, 8.5f, 0f, true, false, 0f, 17f)
            arcToRelative(8.5f, 8.5f, 0f, true, false, 0f, -17f)
            close()
        }
    }
}

private val Sun: ImageVector by lazy {
    symbol("Sun") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 7f)
            arcToRelative(5f, 5f, 0f, true, false, 0f, 10f)
            arcToRelative(5f, 5f, 0f, true, false, 0f, -10f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(12f, 1.5f); lineTo(12f, 4f)
            moveTo(12f, 20f); lineTo(12f, 22.5f)
            moveTo(1.5f, 12f); lineTo(4f, 12f)
            moveTo(20f, 12f); lineTo(22.5f, 12f)
            moveTo(4.6f, 4.6f); lineTo(6.4f, 6.4f)
            moveTo(17.6f, 17.6f); lineTo(19.4f, 19.4f)
            moveTo(19.4f, 4.6f); lineTo(17.6f, 6.4f)
            moveTo(6.4f, 17.6f); lineTo(4.6f, 19.4f)
        }
    }
}

private val Heart: ImageVector by lazy {
    symbol("Heart") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 20.5f)
            curveTo(6f, 16.3f, 3f, 12.8f, 3f, 9.3f)
            arcToRelative(4.6f, 4.6f, 0f, false, true, 9f, -1.9f)
            arcToRelative(4.6f, 4.6f, 0f, false, true, 9f, 1.9f)
            curveTo(21f, 12.8f, 18f, 16.3f, 12f, 20.5f)
            close()
        }
    }
}

/** Three lines with a marker each: a list reads as a list even at 20dp. */
private val ListLines: ImageVector by lazy {
    symbol("ListLines") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(9f, 7f); lineTo(20f, 7f)
            moveTo(9f, 12f); lineTo(20f, 12f)
            moveTo(9f, 17f); lineTo(20f, 17f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 5.6f); arcToRelative(1.4f, 1.4f, 0f, true, false, 0f, 2.8f)
            arcToRelative(1.4f, 1.4f, 0f, true, false, 0f, -2.8f); close()
            moveTo(5f, 10.6f); arcToRelative(1.4f, 1.4f, 0f, true, false, 0f, 2.8f)
            arcToRelative(1.4f, 1.4f, 0f, true, false, 0f, -2.8f); close()
            moveTo(5f, 15.6f); arcToRelative(1.4f, 1.4f, 0f, true, false, 0f, 2.8f)
            arcToRelative(1.4f, 1.4f, 0f, true, false, 0f, -2.8f); close()
        }
    }
}

private val NoteSheet: ImageVector by lazy {
    symbol("NoteSheet") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5.5f, 3.5f)
            lineTo(18.5f, 3.5f)
            lineTo(18.5f, 20.5f)
            lineTo(5.5f, 20.5f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(8.5f, 8f); lineTo(15.5f, 8f)
            moveTo(8.5f, 12f); lineTo(15.5f, 12f)
            moveTo(8.5f, 16f); lineTo(13f, 16f)
        }
    }
}

/**
 * A handset, not a smartphone outline. The tile means "reach this person", and a rectangle reads
 * as a device rather than as a call.
 */
private val Phone: ImageVector by lazy {
    symbol("Phone") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(7f, 3.5f)
            lineTo(10f, 7.5f)
            lineTo(8f, 10f)
            curveTo(9.2f, 12.6f, 11.4f, 14.8f, 14f, 16f)
            lineTo(16.5f, 14f)
            lineTo(20.5f, 17f)
            lineTo(17.5f, 20.5f)
            curveTo(10.5f, 20f, 4f, 13.5f, 3.5f, 6.5f)
            close()
        }
    }
}

private val Clock: ImageVector by lazy {
    symbol("Clock") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = STROKE) {
            moveTo(12f, 3f)
            arcToRelative(9f, 9f, 0f, true, false, 0f, 18f)
            arcToRelative(9f, 9f, 0f, true, false, 0f, -18f)
            close()
        }
        // Deliberately not mirrored in right-to-left layouts: a clock face runs the same way
        // everywhere, and mirroring one is the classic localization mistake.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15.5f, 14f)
        }
    }
}

private val House: ImageVector by lazy {
    symbol("House") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(3.5f, 11f)
            lineTo(12f, 3.8f)
            lineTo(20.5f, 11f)
            lineTo(20.5f, 20.5f)
            lineTo(3.5f, 20.5f)
            close()
        }
    }
}

private val Bowl: ImageVector by lazy {
    symbol("Bowl") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 11f)
            lineTo(21f, 11f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 3f, y1 = 11f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(9f, 7.5f); curveTo(9f, 6f, 10.5f, 5.5f, 10.5f, 4f)
            moveTo(14f, 7.5f); curveTo(14f, 6f, 15.5f, 5.5f, 15.5f, 4f)
        }
    }
}

private val Star: ImageVector by lazy {
    symbol("Star") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            lineTo(14.23f, 8.93f)
            lineTo(20.56f, 9.22f)
            lineTo(15.61f, 13.17f)
            lineTo(17.29f, 19.28f)
            lineTo(12f, 15.8f)
            lineTo(6.71f, 19.28f)
            lineTo(8.39f, 13.17f)
            lineTo(3.44f, 9.22f)
            lineTo(9.77f, 8.93f)
            close()
        }
    }
}
