package app.larova.core.platform

/**
 * The two things Larova hands to another app.
 *
 * Both are handovers, never actions the app completes itself. That is a deliberate limit rather
 * than a shortcut: an app that dials by itself needs the `CALL_PHONE` permission, turns a mistaken
 * tap into a real call, and starts to look like something that alerts emergency services. Handing
 * the number to the phone app with the dialler open is harmless if triggered by accident and needs
 * no permission at all.
 */
interface ExternalActions {

    /**
     * Opens the phone app with [number] filled in. The person taps the call button, not Larova.
     */
    fun prepareCall(number: String)

    /**
     * Opens [url] in the browser. Only `http` and `https` are honoured — see [isOpenableUrl].
     */
    fun openUrl(url: String)
}

/**
 * Whether a stored string is something this app is willing to hand to the system.
 *
 * A tile's contents are typed by a parent, but an import can come from anywhere, and `Intent` will
 * cheerfully act on schemes that have nothing to do with a website. Restricting to http and https
 * means a website tile can only ever open a website.
 */
fun isOpenableUrl(url: String): Boolean {
    val trimmed = url.trim()
    val scheme = trimmed.substringBefore("://", missingDelimiterValue = "").lowercase()
    return !trimmed.contains(WHITESPACE) &&
        (scheme == "http" || scheme == "https") &&
        trimmed.substringAfter("://").isNotEmpty()
}

/**
 * Strips a number down to what a dialler accepts: digits, the leading plus, and the separators a
 * person is likely to have typed. Everything else is dropped rather than passed on.
 */
fun sanitizePhoneNumber(raw: String): String {
    val trimmed = raw.trim()
    val dialable = trimmed.filter { it.isDigit() || it in "*#" }
    // The plus only means anything at the front. A stray one in the middle is a typo, not an
    // international prefix, and passing it on would change the number that gets dialled.
    return if (trimmed.startsWith("+") && dialable.isNotEmpty()) "+$dialable" else dialable
}

private val WHITESPACE = Regex("\\s")
