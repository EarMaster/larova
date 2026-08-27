package app.larova.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The active mode, readable anywhere below [LarovaTheme]. Tile colours need it: a tile stores a
 * token key and the surface it is painted with depends on the mode, which is the whole reason
 * the key is stored rather than the value.
 */
val LocalAppMode = staticCompositionLocalOf { AppMode.LIGHT }

/** Surfaces and ink for the active mode, beyond what Material's ColorScheme has slots for. */
val LocalSurfaces = staticCompositionLocalOf { LightSurfaces }

@Composable
fun LarovaTheme(
    mode: AppMode = if (isSystemInDarkTheme()) AppMode.DARK else AppMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val surfaces = surfacesFor(mode)
    CompositionLocalProvider(
        LocalAppMode provides mode,
        LocalSurfaces provides surfaces,
    ) {
        MaterialTheme(
            colorScheme = colorSchemeFor(mode, surfaces),
            typography = LarovaTypography,
            shapes = LarovaShapes,
            content = content,
        )
    }
}

private fun surfacesFor(mode: AppMode): Surfaces = when (mode) {
    AppMode.LIGHT -> LightSurfaces
    AppMode.DARK -> DarkSurfaces
    AppMode.NIGHT -> NightSurfaces
}

/**
 * Material's scheme is derived from the tables in `AppColors.kt` — never from hex values written
 * here. A second place holding a colour is a second place that can drift from the design system,
 * and dark and night would be the modes where nobody notices.
 */
private fun colorSchemeFor(mode: AppMode, surfaces: Surfaces) = when (mode) {
    AppMode.LIGHT -> lightColorScheme(
        primary = Signal.amberOnLight,
        onPrimary = Color.White,
        secondary = surfaces.ink,
        onSecondary = surfaces.background,
        background = surfaces.background,
        onBackground = surfaces.ink,
        surface = surfaces.raised,
        onSurface = surfaces.ink,
        surfaceVariant = surfaces.background,
        onSurfaceVariant = surfaces.inkMuted,
        outline = surfaces.inkMuted,
        // The help bar is the only thing in the product allowed to be alarm red, and Material's
        // error role is where a component would reach for it by accident. Mapping it here keeps
        // the two in step instead of letting a stray `MaterialTheme.colorScheme.error` invent a
        // second red.
        error = Signal.alarmLight,
        onError = Color.White,
    )

    AppMode.DARK -> darkColorScheme(
        primary = Signal.amberOnDark,
        onPrimary = Signal.amberInk,
        secondary = surfaces.ink,
        onSecondary = surfaces.background,
        background = surfaces.background,
        onBackground = surfaces.ink,
        surface = surfaces.raised,
        onSurface = surfaces.ink,
        surfaceVariant = surfaces.raised,
        onSurfaceVariant = surfaces.inkMuted,
        outline = surfaces.inkMuted,
        error = Signal.alarmDark,
        onError = Signal.alarmDarkInk,
    )

    AppMode.NIGHT -> darkColorScheme(
        primary = Signal.amberOnDark,
        onPrimary = Signal.amberInk,
        secondary = surfaces.ink,
        onSecondary = surfaces.background,
        background = surfaces.background,
        onBackground = surfaces.ink,
        surface = surfaces.raised,
        onSurface = surfaces.ink,
        surfaceVariant = surfaces.raised,
        onSurfaceVariant = surfaces.inkMuted,
        outline = surfaces.inkMuted,
        error = Signal.alarmDark,
        onError = Signal.alarmDarkInk,
    )
}
