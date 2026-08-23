package app.larova.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

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
