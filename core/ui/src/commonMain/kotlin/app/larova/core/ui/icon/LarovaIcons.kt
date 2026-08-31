package app.larova.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icons drawn in code rather than pulled from an icon pack.
 *
 * `autoMirror` is the reason. Arabic is in the launch set, and a back arrow that keeps pointing
 * left in a right-to-left layout is the kind of detail that makes an app feel translated rather
 * than localized. Clocks, phone numbers and media controls are deliberately not mirrored, so this
 * has to be decided per icon rather than globally.
 */
val BackArrow: ImageVector by lazy {
    ImageVector.Builder(
        name = "BackArrow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }.build()
}

/**
 * The overflow menu. Not mirrored: a vertical row of dots has no direction to reverse, and
 * `autoMirror` on something symmetrical is how a mirrored clock face ends up shipping.
 */
val MoreVertical: ImageVector by lazy {
    ImageVector.Builder(
        name = "MoreVertical",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        for (y in listOf(6f, 12f, 18f)) {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, y - 2f)
                arcToRelative(2f, 2f, 0f, true, true, 0f, 4f)
                arcToRelative(2f, 2f, 0f, true, true, 0f, -4f)
                close()
            }
        }
    }.build()
}

/**
 * Settings: three sliders rather than a gear.
 *
 * A gear drawn at this weight is a circle with short rays around it, which is the sun tile
 * symbol with extra steps — and two icons that read the same at 24dp are worse than one that is
 * slightly less conventional. Sliders say "adjust something" and collide with nothing else here.
 */
val Sliders: ImageVector by lazy {
    uiIcon("Sliders") {
        line(4f, 7f, 20f, 7f)
        line(4f, 12f, 20f, 12f)
        line(4f, 17f, 20f, 17f)
        knob(9f, 7f)
        knob(15f, 12f)
        knob(7f, 17f)
    }
}

/**
 * The activity log: a clock with an arrow going back round it.
 *
 * Not mirrored. The direction here is "backwards in time", which is the clock's own direction and
 * not the reading direction — a mirrored history arrow in Arabic would point the wrong way round
 * a clock face that did not move.
 */
val History: ImageVector by lazy {
    uiIcon("History") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // An open circle: the gap at the top left is where the arrow comes back in.
            moveTo(4.6f, 9.5f)
            arcTo(8.5f, 8.5f, 0f, true, true, 4.2f, 14.4f)
            // The arrowhead on the loose end, pointing back the way it came.
            moveTo(1.8f, 7.4f)
            lineTo(4.6f, 9.5f)
            lineTo(7.4f, 8.3f)
            // The hands, so it is a clock and not a refresh button.
            moveTo(12f, 7.6f)
            lineTo(12f, 12f)
            lineTo(15.2f, 13.6f)
        }
    }
}

/**
 * Backup and transfer: one arrow out of the phone and one back in.
 *
 * Vertical on purpose. Left and right would have to mirror in Arabic and would then say the
 * opposite of what they said before; up and down mean the same thing in every layout direction.
 */
val Transfer: ImageVector by lazy {
    uiIcon("Transfer") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(8f, 4f)
            lineTo(8f, 15f)
            moveTo(4.5f, 11.5f)
            lineTo(8f, 15f)
            lineTo(11.5f, 11.5f)
            moveTo(16f, 20f)
            lineTo(16f, 9f)
            moveTo(12.5f, 12.5f)
            lineTo(16f, 9f)
            lineTo(19.5f, 12.5f)
        }
    }
}

/**
 * Arranging tiles: one arrow up, one down, which is exactly what that screen does.
 */
val Reorder: ImageVector by lazy {
    uiIcon("Reorder") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(8f, 20f)
            lineTo(8f, 5f)
            moveTo(4.5f, 8.5f)
            lineTo(8f, 5f)
            lineTo(11.5f, 8.5f)
            moveTo(16f, 4f)
            lineTo(16f, 19f)
            moveTo(12.5f, 15.5f)
            lineTo(16f, 19f)
            lineTo(19.5f, 15.5f)
        }
    }
}

/**
 * Writing a backup out: an arrow down into a tray.
 *
 * Down means "out of the app and into a file" here, which is the convention every file dialog on
 * every platform already taught the person holding the phone. Not mirrored — the arrow is vertical.
 */
val SaveFile: ImageVector by lazy {
    uiIcon("SaveFile") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3.5f)
            lineTo(12f, 14.5f)
            moveTo(7.5f, 10f)
            lineTo(12f, 14.5f)
            lineTo(16.5f, 10f)
            moveTo(4.5f, 17f)
            lineTo(4.5f, 20f)
            lineTo(19.5f, 20f)
            lineTo(19.5f, 17f)
        }
    }
}

/** Reading one back in: the same tray, the arrow the other way. */
val OpenFile: ImageVector by lazy {
    uiIcon("OpenFile") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 14.5f)
            lineTo(12f, 3.5f)
            moveTo(7.5f, 8f)
            lineTo(12f, 3.5f)
            lineTo(16.5f, 8f)
            moveTo(4.5f, 17f)
            lineTo(4.5f, 20f)
            lineTo(19.5f, 20f)
            lineTo(19.5f, 17f)
        }
    }
}

/**
 * A padlock, for a tile type that has not been bought yet.
 *
 * Not mirrored: a padlock has no direction, and `autoMirror` on something symmetrical is how a
 * mirrored clock face ends up shipping. Drawn shut rather than open — an open padlock is the icon
 * for "this is unprotected", which is the opposite of what it would mean here.
 */
val Lock: ImageVector by lazy {
    uiIcon("Lock") {
        // The body, as a rounded rectangle traced by hand: the builder has no rounded-rect path,
        // and four arcs at a 2f radius is what the rest of this file would have written.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(6f, 13f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            lineTo(16f, 11f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            lineTo(18f, 18f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
            lineTo(8f, 20f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
            close()
        }
        // The shackle, stopping at the top of the body rather than overlapping it, so the two
        // shapes still read apart at 24dp.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(8.5f, 11f)
            lineTo(8.5f, 7.5f)
            arcToRelative(3.5f, 3.5f, 0f, false, true, 7f, 0f)
            lineTo(15.5f, 11f)
        }
    }
}

/** Every icon above is drawn in the same 24×24 viewport at the same weight. */
private const val STROKE = 2f

private fun uiIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private fun ImageVector.Builder.line(x1: Float, y1: Float, x2: Float, y2: Float) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(x1, y1)
        lineTo(x2, y2)
    }
}

/** Big enough to read as a control rather than as a bullet, at 24dp and at 48dp. */
private const val KNOB_RADIUS = 2.6f

/** A filled dot on a slider track, drawn over the line so the track does not show through. */
private fun ImageVector.Builder.knob(x: Float, y: Float) {
    path(fill = SolidColor(Color.Black)) {
        moveTo(x, y - KNOB_RADIUS)
        arcToRelative(KNOB_RADIUS, KNOB_RADIUS, 0f, true, true, 0f, KNOB_RADIUS * 2)
        arcToRelative(KNOB_RADIUS, KNOB_RADIUS, 0f, true, true, 0f, -KNOB_RADIUS * 2)
        close()
    }
}
