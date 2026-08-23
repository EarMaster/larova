package app.larova.core.domain.export

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.MediaAsset
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
 */
@Serializable
data class ExportContent(
    val boards: List<Board> = emptyList(),
    val cards: List<Card> = emptyList(),
    val media: List<MediaAsset> = emptyList(),
    val log: List<LogEntry> = emptyList(),
) {
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
