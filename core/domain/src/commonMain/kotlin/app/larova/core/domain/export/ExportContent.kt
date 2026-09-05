package app.larova.core.domain.export

import kotlinx.serialization.Serializable

/**
 * `content.json`: everything a family typed, in one object.
 *
 * The four lists are the whole of it. Cards carry their payloads as the JSON strings they are
 * stored as rather than as decoded objects — a payload this version cannot read is copied through
 * a backup untouched, which is what makes an export written by a newer Larova survive a trip
 * through an older one.
 *
 * [log] is present and empty until M2 fills it. A field that appears later is a format change; a
 * field that was always there and was empty is not.
 *
 * [cardText] is the other case, and the distinction is the whole reason that sentence is written
 * down. It was **not** there from the first release, so it is a format change and `schemaVersion`
 * went to 3 with it. See `ExportManifest.CURRENT_SCHEMA_VERSION` for what would otherwise happen
 * to a family handed a newer backup.
 *
 * The four lists hold [ExportRows] types rather than the models the app works with. See that file
 * for why: a container that serializes domain models inherits their Kotlin identifier spelling as
 * its wire format, which is how this one came to write `"type": "GUIDE"`.
 */
@Serializable
data class ExportContent(
    val boards: List<ExportBoard> = emptyList(),
    val cards: List<ExportCard> = emptyList(),
    val media: List<ExportMediaAsset> = emptyList(),
    val log: List<ExportLogEntry> = emptyList(),
    val cardText: List<ExportCardText> = emptyList(),
) {
    /**
     * Translations are deliberately not counted here. These three numbers are what the import
     * preview shows somebody before they commit to replacing everything they have, and they are
     * there to answer "is this the right backup" — which a count of translations does not help
     * with. The manifest's shape is also frozen; this is not a field to add lightly.
     */
    val counts: ExportCounts
        get() = ExportCounts(boards = boards.size, cards = cards.size, media = media.size)
}

/** How an import applies a package to an installation that already has content. */
enum class ImportMode {
    /**
     * Everything here is thrown away and replaced by what is in the file. What the parents mean by
     * "restore my backup".
     */
    REPLACE,

    /**
     * The file's tiles are added to what is already here. What the parents mean by "the other
     * grandparent sent me their tiles". Tiles with an identifier that is already present are
     * updated rather than duplicated, so importing the same package twice is not twice the tiles.
     */
    MERGE,
}
