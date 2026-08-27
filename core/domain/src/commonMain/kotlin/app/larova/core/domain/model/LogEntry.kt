package app.larova.core.domain.model

import app.larova.core.domain.serialization.InstantSerializer
import app.larova.core.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * One line in the activity log.
 *
 * The log stays a plain event list. It is never scored, trended or interpreted — that is the line
 * between a notebook and a medical device (docs/concept.md §2.2), and it is also what the parents
 * actually want: what happened, when.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class LogEntry(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    @Serializable(with = InstantSerializer::class) val at: Instant,
    val kind: LogKind,
    @Serializable(with = UuidSerializer::class) val cardId: Uuid? = null,
    val note: String? = null,
)

/** [key] is written to export files, so these strings are frozen like the card types are. */
enum class LogKind(val key: String) {
    CARD_OPENED("cardOpened"),
    CHECK_TOGGLED("checkToggled"),
    CALL_PREPARED("callPrepared"),
    MANUAL_NOTE("manualNote"),
    ;

    companion object {
        /** Unknown kinds from a newer export are skipped, not fatal. */
        fun fromKey(key: String?): LogKind? = entries.firstOrNull { it.key == key }
    }
}
