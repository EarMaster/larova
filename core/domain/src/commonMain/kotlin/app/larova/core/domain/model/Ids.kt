package app.larova.core.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Identifiers cross two boundaries as text — the database stores them as TEXT and navigation
 * routes carry them as strings — so parsing them is a normal operation rather than an exceptional
 * one, and failing to parse is an expected answer rather than a crash.
 *
 * One place for it, so that a malformed identifier behaves the same whether it came from a stale
 * back stack entry or from a row written by something else.
 */
@OptIn(ExperimentalUuidApi::class)
fun parseUuidOrNull(raw: String?): Uuid? {
    if (raw.isNullOrEmpty()) return null
    return try {
        Uuid.parse(raw)
    } catch (_: IllegalArgumentException) {
        null
    }
}
