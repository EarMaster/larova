package app.larova.core.domain.usecase

/**
 * The six starting points from `docs/concept.md` §4.6.
 *
 * A first run with an empty screen is a first run that asks "and now what?". These answer it twice
 * over: they put something real on the grid, and they show what a tile can be — a guide, a list, a
 * table, a number — without anybody having to read about it first.
 *
 * The enum is the structure and nothing else. Every word in a template is a translatable string
 * resolved in the UI, because a template is content the moment it is used: it is copied into the
 * database as the parents' own, and from then on it does not follow the app language
 * (`docs/localization.md` §4). A parent who picks "Bedtime" in German and later switches the app to
 * Turkish keeps the German words they have since rewritten, which is the only correct behaviour —
 * they are theirs now.
 */
enum class TemplateId(val key: String) {
    BEDTIME("bedtime"),
    EVENING("evening"),
    CONTACTS("contacts"),
    FOOD("food"),
    DAY("day"),
    WHAT_HELPS("whatHelps"),
}

/**
 * Puts a template on the start screen.
 *
 * The drafts arrive already filled in, because the words are the UI's to resolve and the writing is
 * this layer's. Saved one at a time through the same use case the editor uses, so a template tile is
 * a tile like any other from the moment it exists: same validation, same sort order at the end of
 * the board, nothing marking it as having come from a template.
 *
 * That last part is deliberate. There is no "example" flag to filter on later, no "restore
 * template" to undo an edit. A template is a starting point, and a starting point that keeps
 * following you is a worse one.
 */
class ApplyTemplate(private val saveCard: SaveCard) {

    /** How many tiles were written. Zero means every draft was refused, which nothing should do. */
    suspend operator fun invoke(drafts: List<CardDraft>): Int =
        drafts.count { saveCard(it) is SaveCard.Result.Saved }
}
