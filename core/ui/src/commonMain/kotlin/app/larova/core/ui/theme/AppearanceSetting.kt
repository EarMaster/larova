package app.larova.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * What the user chose in settings — including "follow the phone", which is the default.
 *
 * Distinct from [AppMode], which is what that choice resolves to once the phone's own dark setting
 * is known. Only [AppMode] reaches the colour tables, so night can never be reached by accident:
 * it is a deliberate choice for reading aloud in a dark room, not a system state.
 */
enum class AppearanceSetting { SYSTEM, LIGHT, DARK, NIGHT }

@Composable
@ReadOnlyComposable
fun AppearanceSetting.resolve(): AppMode = when (this) {
    AppearanceSetting.LIGHT -> AppMode.LIGHT
    AppearanceSetting.DARK -> AppMode.DARK
    AppearanceSetting.NIGHT -> AppMode.NIGHT
    AppearanceSetting.SYSTEM -> if (isSystemInDarkTheme()) AppMode.DARK else AppMode.LIGHT
}
