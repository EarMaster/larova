package app.larova.core.domain.model

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * One tile.
 *
 * [icon] and [colorToken] hold **keys**, never values. The theme resolves a key per appearance
 * mode; a stored hex value would make every user-created tile unreadable in dark mode,
 * retroactively and unrepairably, because the values were chosen by users.
 *
 * [payload] is serialized JSON of a [CardPayload]. That is what lets a new tile type ship without
 * a database migration, and what lets a payload from a newer version be skipped rather than bring
 * an entire import down.
 *
 * Not `@Serializable`, and that is the point. A file format that borrows its fields from a domain
 * model is one ordinary refactoring away from changing what every backup contains — which is how
 * this class came to write `"type": "GUIDE"` into export files while the database column said
 * `guide`. The wire shape lives in `export/ExportRows.kt` now, and the missing annotation here is
 * what turns "someone serialized a domain model again" into a compile error rather than a format
 * change nobody notices until a family cannot open their backup.
 */
@OptIn(ExperimentalUuidApi::class)
data class Card(
    val id: Uuid,
    val boardId: Uuid,
    val title: String,
    val subtitle: String? = null,
    val icon: String,
    val colorToken: String,
    val sortIndex: Int,
    val visibleToCaregiver: Boolean = true,
    val type: CardType,
    val payload: String,
    /** Reserved for a per-card second language. Unused in v1, written to exports from the start. */
    val locale: String? = null,
    val updatedAt: Instant,
)

/**
 * The tile types from docs/concept.md §4.1.
 *
 * [key] is the value stored in the database and in every export file, and it matches the payload's
 * own discriminator one for one. All ten are declared from the first release even though M1 renders
 * five of them: freezing the strings costs nothing now and cannot be done later.
 */
enum class CardType(val key: String) {
    GUIDE("guide"),
    NOTE("note"),
    CHECKLIST("checklist"),
    TABLE("table"),
    VIDEO("video"),
    AUDIO("audio"),
    PHONE("phone"),
    WEB("web"),
    APP_LINK("appLink"),
    FOLDER("folder"),
    ;

    companion object {
        /**
         * Null for anything unrecognised, so an import written by a newer version skips that one
         * tile instead of failing — which the export container makes true by writing this key as a
         * plain string, so an unfamiliar one costs one tile rather than the whole file. An unknown
         * type is never mapped onto a known one: a tile that quietly became something else is
         * worse than a tile that is missing.
         */
        fun fromKey(key: String?): CardType? = entries.firstOrNull { it.key == key }
    }
}
