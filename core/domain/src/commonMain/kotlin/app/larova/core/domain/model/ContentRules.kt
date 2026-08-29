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

/**
 * A call tile, with its two halves kept in step.
 *
 * Built here rather than in the editor for the same reason a table is squared off here: an import
 * can come from anywhere, and the rules that make a tile renderable have to apply to a file
 * somebody was sent as much as to one typed on this phone.
 *
 * The first person is written into the flat `displayName`/`number`/`relation`/`inHelpSheet` fields
 * as well as into the list. That is what a version before 0.3.0 reads, and it is the whole of the
 * compatibility story: an older Larova opens a four-number tile and shows the first number instead
 * of failing to decode it. Losing three numbers is a bad afternoon; a tile that will not open is a
 * caregiver who cannot reach anybody.
 *
 * Entries with no number at all are dropped — a name with nothing to dial is a row that does
 * nothing when it is pressed, which on this tile is worse than an absence.
 *
 * No cap on how many. A guide takes as many steps as a bedtime has and a checklist as many items
 * as a nursery bag holds, and a family's numbers are the same kind of list: whoever is writing it
 * knows how long it needs to be, and a limit picked here would be a guess about their household.
 * The help sheet is where a limit *does* belong, and it has one — four, because that list is read
 * in an emergency rather than written at a kitchen table.
 */
fun phoneOf(contacts: List<PhoneEntry>): CardPayload.Phone {
    val cleaned = contacts.asSequence()
        .map { entry ->
            entry.copy(
                displayName = entry.displayName.trim(),
                number = entry.number.trim(),
                relation = entry.relation?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
        .filter { it.number.isNotBlank() }
        .toList()

    val first = cleaned.firstOrNull() ?: PhoneEntry()
    return CardPayload.Phone(
        displayName = first.displayName,
        number = first.number,
        relation = first.relation,
        inHelpSheet = first.inHelpSheet,
        contacts = cleaned,
    )
}

/**
 * A table, squared off.
 *
 * The renderer reads a cell by its column position, so a stored table has to be rectangular: a row
 * with one cell missing would otherwise put the last value under the wrong heading, which on a
 * tile that says "Zeit | Was" is not a cosmetic problem. Rows are padded and trimmed to the number
 * of columns here rather than in the editor, so an import from anywhere is squared off too.
 *
 * Rows that are entirely empty are dropped. Someone who tapped "add row" and then changed their
 * mind meant no row, and a blank line in a table read at arm's length looks like missing content.
 * A cell left empty in a row that has anything else in it is kept: an empty cell can be the answer.
 */
fun tableOf(columns: List<String>, rows: List<List<String>>): CardPayload.Table {
    val trimmedColumns = columns.map { it.trim() }.take(MAX_TABLE_COLUMNS)
    if (trimmedColumns.isEmpty()) return CardPayload.Table()

    val squared = rows
        .map { row -> List(trimmedColumns.size) { index -> row.getOrNull(index).orEmpty().trim() } }
        .filter { row -> row.any { it.isNotEmpty() } }

    return CardPayload.Table(columns = trimmedColumns, rows = squared)
}

/**
 * Four columns.
 *
 * Not a storage limit but a legibility one: this is read on a phone, at up to 200 % font scale, by
 * someone who may be standing up. A fifth column does not make the table more useful, it makes
 * every cell in it two words wide.
 */
const val MAX_TABLE_COLUMNS = 4

private val WHITESPACE = Regex("\\s")
