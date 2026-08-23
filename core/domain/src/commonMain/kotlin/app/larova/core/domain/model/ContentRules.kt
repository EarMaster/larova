package app.larova.core.domain.model

/**
 * What Larova is willing to do with a string a person typed.
 *
 * These live in the domain rather than next to the intent that uses them, because the editor has
 * to apply exactly the same rules when validating: a web address the editor accepts and the opener
 * refuses is a tile that does nothing when tapped, and the parent who made it is not there to see
 * it happen.
 */

/**
 * Whether a stored string is something the app will hand to a browser.
 *
 * Tile contents are typed by a parent, but an import can come from anywhere, and an Android intent
 * will cheerfully act on schemes that have nothing to do with a website. Restricting to http and
 * https means a website tile can only ever open a website.
 */
fun isOpenableUrl(url: String): Boolean {
    val trimmed = url.trim()
    val scheme = trimmed.substringBefore("://", missingDelimiterValue = "").lowercase()
    return !trimmed.contains(WHITESPACE) &&
        (scheme == "http" || scheme == "https") &&
        trimmed.substringAfter("://").isNotEmpty()
}

/**
 * Strips a number down to what a dialler accepts: digits, the separators a person is likely to
 * have typed, and a leading plus. Everything else is dropped rather than passed on.
 */
fun sanitizePhoneNumber(raw: String): String {
    val trimmed = raw.trim()
    val dialable = trimmed.filter { it.isDigit() || it in "*#" }
    // The plus only means anything at the front. A stray one in the middle is a typo, not an
    // international prefix, and passing it on would change the number that gets dialled.
    return if (trimmed.startsWith("+") && dialable.isNotEmpty()) "+$dialable" else dialable
}

private val WHITESPACE = Regex("\\s")
