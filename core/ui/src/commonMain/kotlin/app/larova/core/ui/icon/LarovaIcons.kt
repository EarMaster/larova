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
