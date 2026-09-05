package app.larova.core.domain.repository

import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.Entitlement
import app.larova.core.domain.model.LastBackup
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.model.Receipt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/**
 * The contracts the data layer implements and the use cases depend on.
 *
 * They live in `:core:domain` so that the direction of the arrow is fixed: UI reads state from a
 * ViewModel, the ViewModel calls use cases, use cases call these. Nothing above ever reaches into
 * the data layer, and nothing here knows what a database or a file system is.
 */
@OptIn(ExperimentalUuidApi::class)
interface BoardRepository {
    /** The start screen, which is the board with no parent. Created on first run if absent. */
    fun observeRootBoard(): Flow<Board?>

    fun observeChildren(parentId: Uuid?): Flow<List<Board>>

    suspend fun find(id: Uuid): Board?

    /** Every board. Needed to replace an installation wholesale, and to find the start screen. */
    suspend fun all(): List<Board>

    suspend fun upsert(board: Board)

    suspend fun delete(id: Uuid)
}

@OptIn(ExperimentalUuidApi::class)
interface CardRepository {
    fun observeCards(boardId: Uuid): Flow<List<Card>>

    /**
     * Every tile, whichever board it is on. The help sheet needs this: a number worth reaching in
     * a hurry might be on a tile inside a folder, and being one level deep must not make it
     * unreachable from the bar.
     */
    fun observeAllCards(): Flow<List<Card>>

    /** Search runs over titles and subtitles only. Payload content is never interpreted. */
    fun search(query: String): Flow<List<Card>>

    suspend fun find(id: Uuid): Card?

    suspend fun upsert(card: Card)

    /** Persists a whole board's order in one write, so a reorder cannot end up half applied. */
    suspend fun reorder(boardId: Uuid, orderedIds: List<Uuid>)

    suspend fun delete(id: Uuid)
}

/**
 * A tile's text in languages other than the one it was written in.
 *
 * Whole-tile rows, never a per-field map: a row carries title, second line and payload together, so
 * there is no state in which half a tile is in one language. Which one to show is `resolveCardText`
 * and nothing else.
 */
@OptIn(ExperimentalUuidApi::class)
interface CardTextRepository {

    /**
     * Every variant there is.
     *
     * Everything at once rather than a query per tile: a family with sixty tiles in two languages
     * has sixty rows, and the start screen resolves a whole board while it is drawing.
     */
    fun observeAll(): Flow<List<CardText>>

    fun observeForCard(cardId: Uuid): Flow<List<CardText>>

    /** Everything, for an export. */
    suspend fun all(): List<CardText>

    suspend fun upsert(text: CardText)

    suspend fun delete(cardId: Uuid, lang: String)

    /**
     * Every variant of one tile. The database cascades this when the tile goes; this exists for the
     * import that clears an installation, where the deletes are written out rather than relied on.
     */
    suspend fun deleteForCard(cardId: Uuid)
}

@OptIn(ExperimentalUuidApi::class)
interface MediaRepository {
    fun observeAll(): Flow<List<MediaAsset>>

    suspend fun find(id: Uuid): MediaAsset?

    suspend fun register(asset: MediaAsset)

    suspend fun delete(id: Uuid)

    /** Removes files no card refers to any more. Media outlives the tile that introduced it. */
    suspend fun deleteOrphans(): Int
}

interface LogRepository {
    fun observeRecent(limit: Int): Flow<List<LogEntry>>

    suspend fun append(entry: LogEntry)

    /** Retention is 30 days by default, adjustable, and applied on the way in as well as out. */
    suspend fun pruneOlderThanDays(days: Int)

    suspend fun clear()
}

/**
 * Settings that are not content: appearance, retention, the parent-view PIN. Deliberately separate
 * from the database, since none of it belongs in an export of a family's tiles.
 */
interface PreferencesRepository {
    fun observeAppearance(): Flow<AppearanceSetting>

    suspend fun setAppearance(setting: AppearanceSetting)

    /** Null until a backup has succeeded once on this installation. */
    fun observeLastBackup(): Flow<LastBackup?>

    suspend fun setLastBackup(backup: LastBackup)

    /**
     * How many times somebody has contributed to the development, on this installation.
     *
     * A tally rather than a fact about the account. The contribution is consumed after each
     * purchase so it can be bought again, which means Play does not remember it and cannot be
     * asked — so this is what there is. It does not survive a reinstall, and that is the cost of
     * the product being repeatable.
     *
     * Here rather than in the database for the same reason as the rest of this interface: it is
     * not content, and it has no business travelling in a backup somebody hands to a grandparent.
     */
    fun observeSupportCount(): Flow<Int>

    /** One more. Never resets — nothing anybody paid for should quietly become zero. */
    suspend fun addSupport()

    /**
     * Which language tiles are shown in when a tile has more than one, or null to follow the app's
     * own language — which is the default and where most installations will stay.
     *
     * A setting for the phone, not for a tile: it is the person holding it who cannot read German,
     * not this particular guide. Here rather than in the database for the same reason as the PIN
     * and the appearance — it is not content, and it has no business travelling in a backup handed
     * to a grandparent whose phone is in another language entirely.
     */
    fun observeContentLanguage(): Flow<String?>

    suspend fun setContentLanguage(tag: String?)
}

/**
 * The parent-view PIN.
 *
 * Only ever a hash crosses this boundary in either direction — the PIN itself is passed in to be
 * checked and is never stored, returned or logged. There is deliberately no way to read it back:
 * a forgotten PIN is answered by setting a new one, not by recovering the old.
 */
interface PinRepository {

    /** Whether parent view has a way in at all. False on a fresh installation. */
    suspend fun hasPin(): Boolean

    suspend fun setPin(pin: String)

    suspend fun verify(pin: String): Boolean

    /** Removes it. Parent view then has no lock, which is a decision the parents get to make. */
    suspend fun clear()
}

/**
 * What this installation is entitled to author.
 *
 * Deliberately the narrowest interface in this file. It cannot be told to unlock: there is no
 * `setUnlocked`, because a screen that could call one would be the shortest path to a paid tier
 * that any crash log could explain how to defeat. The only ways in are a store purchase and a
 * signed key, and both are checked below this line.
 *
 * [refresh] asks the store again and is allowed to *raise* the answer. It is never allowed to lower
 * it on a failure — see the cache-positive rule on [EntitlementCache].
 */
interface EntitlementRepository {

    fun observe(): Flow<Entitlement>

    /**
     * Re-asks whatever can vouch for this installation. Safe to call on every start and safe to
     * fail: an offline phone answers "no idea", which must never read as "not paid".
     */
    suspend fun refresh()

    /**
     * What the unlock costs, already formatted in the buyer's own currency, or null if nobody could
     * be asked — offline, or a build with nothing for sale.
     *
     * A formatted string rather than an amount and a currency code, because the store is the only
     * thing that knows how to write a price for a given country, and a number formatted here would
     * be wrong in a way nobody would notice until a review said so.
     */
    suspend fun formattedPrice(): String?
}

/**
 * Where the evidence for an unlock is remembered between launches.
 *
 * In the preferences file rather than the database, for the same reason the PIN is: it is not
 * content, and a package handed to a grandparent must not carry the parents' purchase with it.
 * Keeping it out of the schema is what makes that true by construction instead of by remembering
 * to exclude it from an export.
 *
 * **The cache-positive rule.** Nothing here is cleared because a lookup failed. A child's phone can
 * be offline for weeks, and the store cannot be reached from a process with no internet permission
 * at all — every unsuccessful query is therefore "unknown", never "unpaid". [clear] exists for a
 * refund the store actually reported and for tests, and for nothing else.
 */
interface EntitlementCache {

    /** Null until something has been stored. Re-checked on the way out, never trusted as stored. */
    fun observe(): Flow<Receipt?>

    suspend fun write(receipt: Receipt)

    /** Only for a revocation the store returned successfully. Not for a failed lookup. */
    suspend fun clear()
}
