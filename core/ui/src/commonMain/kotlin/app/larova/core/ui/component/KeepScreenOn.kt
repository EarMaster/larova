package app.larova.core.ui.component

import androidx.compose.runtime.Composable

/**
 * Keeps the display awake while this composable is in the composition.
 *
 * Used on the guide screen and nowhere else. Someone following five steps with their hands full
 * should not have to wake the phone between them, and a screen that dims halfway through a bedtime
 * routine is exactly the moment the app is meant to help with.
 *
 * Scoped rather than global on purpose: the flag is cleared when the screen is left, so a phone
 * left on the start screen still sleeps.
 */
@Composable
expect fun KeepScreenOn()
