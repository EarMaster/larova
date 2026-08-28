package app.larova.core.domain.model

import kotlin.time.Instant

/**
 * When a backup last succeeded, and how much was in it.
 *
 * Kept in preferences rather than in the database, and deliberately not in an export: it describes
 * this installation's own history, not the family's content, so restoring a backup onto a new phone
 * must not claim that phone was backed up in March.
 *
 * It exists because "have I ever actually done this?" is the question a parent has about backup,
 * and it is the one question the app can answer without asking them to go and look in a folder.
 * It is a record of the app's own action, never of what anybody read — the log is where events
 * about a person go, and this is not one.
 */
data class LastBackup(
    val at: Instant,
    val cards: Int,
    val media: Int,
)
