package app.larova.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3's scale with the two adjustments from docs/design/design-system.md §4.
 *
 * Body never drops below 16sp: part of the audience is over 65 and reading in dim light. Sizes are
 * in sp throughout so the system font scale applies — up to 200 %, which every layout has to
 * survive without clipping.
 *
 * The system font is used deliberately. A bundled display face would need Latin, Cyrillic, Arabic,
 * Devanagari, Han and Japanese coverage, and no single reasonable file has it.
 */
val LarovaTypography = Typography().let { base ->
    base.copy(
        bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 23.sp),
        bodySmall = base.bodySmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
        labelLarge = base.labelLarge.copy(fontSize = 16.sp),
        titleMedium = base.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    )
}

/**
 * Guide steps are read aloud, in dim light, often by someone who did not choose this phone.
 * 22sp is a floor, not a suggestion, and it has no slot in the Material scale.
 */
val GuideStepStyle: TextStyle = TextStyle(
    fontSize = 22.sp,
    lineHeight = 32.sp,
    fontWeight = FontWeight.Normal,
)
