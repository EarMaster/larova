package app.larova.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import app.larova.core.domain.model.AppearanceSetting

/**
 * Turns the stored setting into the mode the colour tables are keyed by.
 *
 * The setting lives in `:core:domain` because it is persisted; the resolution lives here because
 * it needs to ask the phone what it is currently doing. Only [AppMode] reaches the tables, which
 * is why night can never be arrived at by following the system: it is a deliberate choice for
 * reading aloud in a darkened room.
 */
@Composable
@ReadOnlyComposable
fun AppearanceSetting.resolve(): AppMode = when (this) {
    AppearanceSetting.LIGHT -> AppMode.LIGHT
    AppearanceSetting.DARK -> AppMode.DARK
    AppearanceSetting.NIGHT -> AppMode.NIGHT
    AppearanceSetting.SYSTEM -> if (isSystemInDarkTheme()) AppMode.DARK else AppMode.LIGHT
}
