package app.larova.core.platform

import android.content.Intent
import android.content.pm.PackageManager

/**
 * The two ways Android has of asking another app to translate something, in the order to try them.
 *
 * One object, because the question "can anything take this?" and the act of handing it over have to
 * agree about the intents exactly. Two lists built separately is how a control appears on the
 * screen and then opens nothing.
 *
 * No `Build.VERSION` branching, which is deliberate. `ACTION_TRANSLATE` arrived in Android 10, but
 * an SDK check would be answering the wrong question: what matters is whether an app on *this*
 * phone answers, and a phone on Android 11 with a translator that only registers for
 * `ACTION_PROCESS_TEXT` is an ordinary device. Resolution decides; the version does not.
 */
internal object TranslateIntents {

    /**
     * `ACTION_PROCESS_TEXT` matches text handlers in general, not only translators, so on a phone
     * with no translation app the second candidate can open a dictionary or a search box instead.
     * That is accepted: `ACTION_TRANSLATE` is tried first and is where every phone that has a
     * translator lands, and refusing to fall back would mean the feature simply does not exist
     * below Android 10.
     */
    fun candidates(text: String): List<Intent> = listOf(
        // No `type`: ACTION_TRANSLATE takes its text in an extra and setting a MIME type on it
        // narrows the match for no benefit.
        Intent(Intent.ACTION_TRANSLATE).putExtra(Intent.EXTRA_TEXT, text),
        // Read-only, because nothing here can receive an answer: the hand-off is launched from the
        // application context, which has no activity to return a result to. The person copies what
        // they want out of the translator themselves.
        Intent(Intent.ACTION_PROCESS_TEXT)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true),
    )

    /**
     * The first candidate something on this phone will answer, or null if nothing will.
     *
     * The plain `0` flags argument and the absence of any version branching match
     * `AndroidInstalledApps`: visibility is declared once in the manifest's `<queries>` element,
     * which lists these same two actions. Without that element this returns null on Android 11 and
     * later rather than failing, and the control quietly never appears — so the two belong
     * together, and the manifest says so.
     */
    fun firstResolvable(manager: PackageManager, candidates: List<Intent>): Intent? =
        candidates.firstOrNull { manager.queryIntentActivities(it, 0).isNotEmpty() }
}
