package app.larova.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import app.larova.core.domain.model.isOpenableUrl
import app.larova.core.domain.model.sanitizePhoneNumber

/**
 * `ACTION_DIAL`, never `ACTION_CALL`. The dialler opens with the number in it and the person
 * decides — which is why Larova needs no `CALL_PHONE` permission and why a tile tapped by mistake
 * costs nothing.
 *
 * A missing handler is not an error worth crashing on: a tablet with no dialler and no browser is
 * a perfectly ordinary device for this app to be installed on.
 */
class AndroidExternalActions(private val context: Context) : ExternalActions {

    override fun prepareCall(number: String) {
        val sanitized = sanitizePhoneNumber(number)
        if (sanitized.isEmpty()) return
        // Uri.encode, because a number can legitimately contain '#' and Uri would read it as a
        // fragment — the dialler would then open with the digits after it missing.
        launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(sanitized))))
    }

    override fun openUrl(url: String) {
        if (!isOpenableUrl(url)) return
        launch(Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())))
    }

    override fun openApp(packageName: String) {
        if (packageName.isBlank()) return
        // Null for an app that is gone, disabled, or has no launcher entry — all three of which are
        // ordinary things to happen to a tile made months ago. The screen has already asked and
        // will be showing that instead of a button, so there is nothing to report here.
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch(intent)
    }

    /**
     * `ACTION_APP_LOCALE_SETTINGS` arrived in Android 13. The constant itself is a compile-time
     * string and is safe to name at any minSdk; what does not exist below 13 is the screen.
     */
    override val canOpenAppLanguageSettings: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun openAppLanguageSettings() {
        if (!canOpenAppLanguageSettings) return
        // `package:` rather than an extra: this is the documented way to name which app's languages
        // the screen should show, and without it the intent opens the phone's whole language list.
        launch(
            Intent(
                Settings.ACTION_APP_LOCALE_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }

    private fun launch(intent: Intent) {
        // The application context has no task of its own to start an activity in.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Nothing on this device can take it. Silence is the right answer: there is nothing
            // the person reading a tile could do about it.
        }
    }
}
