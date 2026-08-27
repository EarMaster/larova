package app.larova.core.domain.usecase

import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.LogKind
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.LogRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Writes what happened.
 *
 * Four kinds and nothing else: a tile was opened, something was ticked off, a call was prepared, or
 * somebody wrote a line themselves. Never a measurement, never a score, never a total — the log is
 * a list of events and stays one, which is the line in `docs/concept.md` §2.2 between a notebook
 * and a medical device. For the parents this is the documentation feature; it earns that by being
 * literal.
 *
 * Nothing here fails loudly. A log write that could not happen must not take a tile down with it:
 * the log is a record of what the app did, not part of doing it.
 */
@OptIn(ExperimentalUuidApi::class)
class RecordEvent(private val log: LogRepository) {

    suspend fun cardOpened(cardId: Uuid) = append(LogKind.CARD_OPENED, cardId = cardId)

    suspend fun checkToggled(cardId: Uuid) = append(LogKind.CHECK_TOGGLED, cardId = cardId)

    suspend fun callPrepared(cardId: Uuid?) = append(LogKind.CALL_PREPARED, cardId = cardId)

    /**
     * A line a caregiver wrote.
     *
     * The one entry that is not the app describing itself, and the reason the log is worth having:
     * "he would not eat lunch" is what the parents come home to read. Blank notes are dropped —
     * there is nothing to record and an empty row would only make the list harder to scan.
     */
    suspend fun note(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        append(LogKind.MANUAL_NOTE, note = trimmed)
        return true
    }

    private suspend fun append(kind: LogKind, cardId: Uuid? = null, note: String? = null) {
        log.append(
            LogEntry(
                id = Uuid.random(),
                at = Clock.System.now(),
                kind = kind,
                cardId = cardId,
                note = note,
            ),
        )
    }
}

/**
 * One line of the log with the tile it refers to already resolved.
 *
 * The title rather than the identifier, because that is the only part anybody reads — and null when
 * the tile has since been deleted, which the screen says in words rather than showing a bare
 * identifier nobody can place.
 */
@OptIn(ExperimentalUuidApi::class)
data class LogLine(
    val id: Uuid,
    val at: kotlin.time.Instant,
    val kind: LogKind,
    val cardTitle: String?,
    val note: String?,
)

/**
 * The log, newest first, with tile titles filled in.
 *
 * Combined with the tiles rather than joined in the database: the log stores an identifier so that
 * renaming a tile does not rewrite history, and a deleted tile leaves its entries standing. What
 * happened still happened.
 *
 * Bounded by [LOG_LIMIT]. A month of a busy family is a long list, and the screen is read by
 * scrolling rather than by searching.
 */
@OptIn(ExperimentalUuidApi::class)
class ObserveLog(
    private val log: LogRepository,
    private val cards: CardRepository,
) {

    operator fun invoke(): Flow<List<LogLine>> =
        log.observeRecent(LOG_LIMIT).combine(cards.observeAllCards()) { entries, allCards ->
            val titles = allCards.associate { it.id to it.title }
            entries.map { entry ->
                LogLine(
                    id = entry.id,
                    at = entry.at,
                    kind = entry.kind,
                    cardTitle = entry.cardId?.let { titles[it] },
                    note = entry.note,
                )
            }
        }
}

/** Empties the log. Parent view only, and there is no undo — the screen says so first. */
class ClearLog(private val log: LogRepository) {

    suspend operator fun invoke() = log.clear()
}

/**
 * Drops what is older than the retention window.
 *
 * Run at startup rather than on a schedule: an offline app with no background work has exactly one
 * reliable moment to tidy up, and that is when somebody opens it. Applied to what an import brings
 * in as well, so restoring a two-year-old backup does not resurrect two years of events.
 */
class PruneLog(private val log: LogRepository) {

    suspend operator fun invoke(days: Int = LOG_RETENTION_DAYS) = log.pruneOlderThanDays(days)
}

/**
 * Thirty days, as `docs/concept.md` §4.4 specifies.
 *
 * Long enough to answer "how did the last few weeks go" and short enough that the log never becomes
 * a second archive of a child's life sitting on a phone. Making it adjustable is a setting and a
 * stored preference; the number being the documented default is what matters first.
 */
const val LOG_RETENTION_DAYS = 30

/** One screen of scrolling, not a database dump. */
private const val LOG_LIMIT = 500
