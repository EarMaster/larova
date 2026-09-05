package app.larova.core.domain.model

/**
 * A tile, flattened to the words on it.
 *
 * This is what Larova hands to a translation app, and it is deliberately the *only* thing it hands
 * over. Three rules decide what it contains, and all three are load-bearing:
 *
 * **Numbers, addresses and package names are never included.** A translator returns Arabic-Indic
 * digits for a phone number and a percent-encoded address, and either pasted back would break the
 * tile in a way nobody notices until somebody needs it. Nothing here is translatable anyway: a
 * number is a number in every language.
 *
 * **No word is ever added.** No "Step 1", no "Column", no label of any kind. `:core:domain` cannot
 * see the string resources and so could not localize such a word even if it wanted to, which is
 * exactly why this stays honest — the only characters here that a parent did not type are newlines
 * and the [CELL_SEPARATOR] between table cells. Adding a numbered prefix would also make the result
 * impossible to paste back one field at a time, which is how a parent uses it.
 *
 * **The blank line is the separator.** Blocks — the title, the second line, each step of a guide —
 * are separated by an empty line; lines within a block by a single newline. That is what lets
 * somebody copy one step out of the translator's answer and paste it into one field.
 */
fun plainTextOf(title: String, subtitle: String?, payload: CardPayload): String {
    val blocks = buildList {
        add(title)
        subtitle?.let { add(it) }
        addAll(payload.textBlocks())
    }
    return blocks
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(BLOCK_SEPARATOR)
        .capped()
}

/**
 * The words on a payload, as blocks.
 *
 * Exhaustive with no `else`, so a tile type added later is a compile error here until somebody
 * decides what its words are. That is the point: a new type that silently handed over nothing
 * would look like a translation feature that does not work on exactly one kind of tile.
 */
private fun CardPayload.textBlocks(): List<String> = when (this) {
    // One block per step, so a blank line separates them and each can be pasted back on its own.
    is CardPayload.Guide -> steps.map { it.text }

    is CardPayload.Note -> listOf(text)

    // One block, one item per line. The ticks are not words: whether the teeth are brushed is
    // this morning's state, not something to translate.
    is CardPayload.Checklist -> listOf(items.joinToString(LINE_SEPARATOR) { it.text })

    // The header row, then the rows, cells joined by a separator. Empty cells are kept rather
    // than dropped, so the columns still line up on the way back and a translator can see which
    // cell belongs to which heading.
    is CardPayload.Table -> listOf(
        (listOf(columns) + rows).joinToString(LINE_SEPARATOR) { it.joinToString(CELL_SEPARATOR) },
    )

    is CardPayload.Video -> listOfNotNull(caption)
    is CardPayload.Audio -> listOfNotNull(caption)

    // Names and how they are related, never the numbers. "Mum", "the practice" and "next door"
    // are the words a caregiver has to understand; the digits beside them are not.
    is CardPayload.Phone -> people.map {
        listOfNotNull(it.displayName.ifBlank { null }, it.relation).joinToString(LINE_SEPARATOR)
    }

    // What the page is for, never the address.
    is CardPayload.Web -> listOfNotNull(label, caption)

    // What the app is for, never the package name.
    is CardPayload.AppLink -> listOfNotNull(label.ifBlank { null }, caption)

    // Nothing of its own. A folder's words are the titles of the tiles inside it, and each of
    // those is a tile somebody can open and translate on its own.
    is CardPayload.Folder -> emptyList()
}

/**
 * Kept short enough to survive being handed to another app.
 *
 * An intent's extras cross a Binder transaction with about a megabyte to share, and going over it
 * throws `TransactionTooLargeException` — which `AndroidExternalActions` does not catch, because it
 * is a programming error rather than a missing app. No tile anybody typed comes close; a tile that
 * arrived in an import was not typed by anybody. Cutting at a block boundary rather than mid-word
 * is the difference between a short translation and a mangled one.
 */
private fun String.capped(): String {
    if (length <= MAX_HANDOFF_CHARS) return this
    val cut = lastIndexOf(BLOCK_SEPARATOR, startIndex = MAX_HANDOFF_CHARS)
    return if (cut > 0) substring(0, cut) else substring(0, MAX_HANDOFF_CHARS)
}

private const val BLOCK_SEPARATOR = "\n\n"

private const val LINE_SEPARATOR = "\n"

/**
 * Between the cells of one table row. A vertical bar because it is not a word in any of the
 * fourteen languages and no translator will try to translate it.
 */
private const val CELL_SEPARATOR = " | "

/** Roughly 120 KB of UTF-16, comfortably inside what a Binder transaction will carry. */
private const val MAX_HANDOFF_CHARS = 60_000

/**
 * The words on a tile that a translation replaces, in a fixed order.
 *
 * A flat list, because a translation editor needs one field per phrase and nothing else: no colour,
 * no symbol, no pictures, no phone numbers. Pairing it with [withTextFields] is what lets one small
 * screen translate all ten tile types without a second copy of the editor — and what makes the
 * "same kind of tile" invariant structural rather than checked, since putting words back can only
 * ever produce the payload it started from.
 *
 * The order matches [plainTextOf] after the title and second line, so somebody translating with the
 * hand-off gets their answer back in the order the fields are in.
 */
fun textFieldsOf(payload: CardPayload): List<String> = when (payload) {
    is CardPayload.Guide -> payload.steps.map { it.text }
    is CardPayload.Note -> listOf(payload.text)
    is CardPayload.Checklist -> payload.items.map { it.text }
    // Headings first, then the rows read across. Empty cells are fields too: a translation with
    // fewer of them would be a table with a different shape.
    is CardPayload.Table -> payload.columns + payload.rows.flatten()
    is CardPayload.Video -> listOf(payload.caption.orEmpty())
    is CardPayload.Audio -> listOf(payload.caption.orEmpty())
    // Names and how they are related. Never the numbers — a translated phone number is a phone
    // number that no longer rings.
    is CardPayload.Phone -> payload.people.flatMap { listOf(it.displayName, it.relation.orEmpty()) }
    is CardPayload.Web -> listOf(payload.label.orEmpty(), payload.caption.orEmpty())
    is CardPayload.AppLink -> listOf(payload.label, payload.caption.orEmpty())
    is CardPayload.Folder -> emptyList()
}

/**
 * The same tile with those words replaced, and nothing else touched.
 *
 * Everything [textFieldsOf] left out is copied straight from [payload] — the pictures a guide
 * points at, the numbers on a call tile, the address behind a website, the folder's board. So a
 * variant is always the same kind of tile with the same structure as the original, whatever
 * somebody typed into the fields.
 *
 * A [values] list of the wrong length is answered with the payload unchanged rather than with a
 * half-applied one. It cannot happen from the editor, which builds the list from this same
 * function; it could from a future caller, and a partly-translated tile is the thing this design
 * exists to prevent.
 */
@Suppress("CyclomaticComplexMethod")
fun withTextFields(payload: CardPayload, values: List<String>): CardPayload {
    if (values.size != textFieldsOf(payload).size) return payload
    val next = values.iterator()
    return when (payload) {
        is CardPayload.Guide -> payload.copy(steps = payload.steps.map { it.copy(text = next.next()) })
        is CardPayload.Note -> payload.copy(text = next.next())
        is CardPayload.Checklist ->
            payload.copy(items = payload.items.map { it.copy(text = next.next()) })
        is CardPayload.Table -> payload.copy(
            columns = payload.columns.map { next.next() },
            rows = payload.rows.map { row -> row.map { next.next() } },
        )
        is CardPayload.Video -> payload.copy(caption = next.next())
        is CardPayload.Audio -> payload.copy(caption = next.next())
        is CardPayload.Phone -> payload.copy(
            contacts = payload.people.map {
                it.copy(displayName = next.next(), relation = next.next())
            },
        )
        is CardPayload.Web -> payload.copy(label = next.next(), caption = next.next())
        is CardPayload.AppLink -> payload.copy(label = next.next(), caption = next.next())
        is CardPayload.Folder -> payload
    }
}
