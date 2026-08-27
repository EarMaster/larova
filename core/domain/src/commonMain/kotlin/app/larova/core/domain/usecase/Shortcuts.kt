package app.larova.core.domain.usecase

import app.larova.core.domain.model.LogKind
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.LogRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

/**
 * A tile worth putting on the home screen, one level above the app itself.
 *
 * The label is the tile's own title. A shortcut called anything else would be a second name for the
 * same thing, learned separately.
 */
@OptIn(ExperimentalUuidApi::class)
data class ShortcutTarget(val cardId: Uuid, val label: String)

/**
 * Where the launcher shortcuts come from: what has actually been opened.
 *
 * Counted from the log rather than from a separate tally, which is the whole reason the log came
 * first. It already records every opening, it is already pruned to thirty days, and a second
 * counter would be a second thing that can disagree about what happened.
 *
 * Three, as `docs/implementation-plan.md` says. A launcher that offers everything offers nothing:
 * the point is that the two or three things a caregiver opens every day are one tap from the home
 * screen instead of two.
 *
 * Ties are broken by the order the tiles are in on the board, so the list does not reshuffle itself
 * between two equally-used tiles every time the app starts — a shortcut that moves is a shortcut
 * nobody trusts.
 */
@OptIn(ExperimentalUuidApi::class)
class MostOpenedTiles(
    private val log: LogRepository,
    private val cards: CardRepository,
) {

    suspend operator fun invoke(limit: Int = SHORTCUT_LIMIT): List<ShortcutTarget> {
        val openings = log.observeRecent(LOG_SAMPLE).first()
            .filter { it.kind == LogKind.CARD_OPENED }
            .mapNotNull { it.cardId }
            .groupingBy { it }
            .eachCount()

        if (openings.isEmpty()) return emptyList()

        return cards.observeAllCards().first()
            .filter { it.id in openings }
            .sortedWith(compareByDescending<app.larova.core.domain.model.Card> { openings[it.id] ?: 0 }
                .thenBy { it.sortIndex })
            .take(limit)
            .map { ShortcutTarget(cardId = it.id, label = it.title) }
    }
}

/** Three. More than that and the launcher menu is a second start screen. */
const val SHORTCUT_LIMIT = 3

/** Enough of the log to count a month of openings without reading a table that only grows. */
private const val LOG_SAMPLE = 2_000

/**
 * Writes the launcher's list of shortcuts.
 *
 * Run at startup, alongside the other housekeeping. An offline app has one reliable moment to bring
 * the outside world up to date about itself, and that is when somebody opens it — and "the three
 * tiles opened most often this month" is not a thing that needs to be current to the minute.
 */
class PublishShortcuts(
    private val mostOpened: MostOpenedTiles,
    private val shortcuts: app.larova.core.domain.app.Shortcuts,
) {

    suspend operator fun invoke() = shortcuts.publish(mostOpened())
}
