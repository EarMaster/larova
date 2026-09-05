package app.larova.core.domain.usecase

import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.canonicalLanguageTag
import app.larova.core.domain.model.cardType
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.CardTextRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Writes one tile's text in one language.
 *
 * The two invariants the resolver deliberately does not enforce are enforced here, because a read
 * that repaired a broken row would hide it rather than refuse it:
 *
 * - **the title is never blank.** `SaveCard` refuses one for the same reason — a tile with no
 *   title is an unreadable square on the grid;
 * - **the payload is the same kind of tile as the card's own.** That is what "never a
 *   half-translated tile" means at the byte level. A variant claiming to be a checklist on a guide
 *   tile survives every test and fails on the screen of the person who opened it.
 *
 * `ImportPackage` refuses both again on its own terms, because a file can come from anywhere and
 * this class is not on that path.
 */
@OptIn(ExperimentalUuidApi::class)
class SaveCardText(
    private val cards: CardRepository,
    private val texts: CardTextRepository,
) {

    sealed interface Result {
        data class Saved(val lang: String) : Result

        /** Nothing to write. Same rule as `SaveCard`, and the same reason. */
        data object TitleMissing : Result

        /** Not a language tag at all, so there is no key to store it under. */
        data object LanguageMissing : Result

        /** The tile went while the translation was being written. */
        data object NoSuchCard : Result

        /** The payload is a different kind of tile from the one it claims to translate. */
        data object WrongPayloadType : Result
    }

    suspend operator fun invoke(
        cardId: Uuid,
        lang: String,
        title: String,
        subtitle: String?,
        payload: String,
    ): Result {
        val tag = canonicalLanguageTag(lang)
        val trimmed = title.trim()
        refusalFor(cardId, tag, trimmed, payload)?.let { return it }

        texts.upsert(
            CardText(
                cardId = cardId,
                lang = requireNotNull(tag),
                title = trimmed,
                subtitle = subtitle?.trim()?.takeIf { it.isNotEmpty() },
                payload = payload,
                // Now, so that a translation written after an edit is not born stale — and so that
                // one written before the next edit becomes stale when that edit happens.
                updatedAt = Clock.System.now(),
            ),
        )
        return Result.Saved(tag)
    }

    /**
     * Why this cannot be written, or null.
     *
     * Gathered into one function rather than four exits from the write, so the write itself reads
     * as one thing and the refusals read as a list of them. `ImportPackage` makes the same checks
     * again on its own terms, because a file can come from anywhere and does not pass through here.
     */
    private suspend fun refusalFor(
        cardId: Uuid,
        tag: String?,
        title: String,
        payload: String,
    ): Result? {
        val card = cards.find(cardId)
        return when {
            tag == null -> Result.LanguageMissing
            title.isEmpty() -> Result.TitleMissing
            card == null -> Result.NoSuchCard
            CardPayloadCodec.decodeOrNull(payload)?.cardType != card.type -> Result.WrongPayloadType
            else -> null
        }
    }
}

/** Removes one language from a tile. The tile and its other languages are untouched. */
@OptIn(ExperimentalUuidApi::class)
class DeleteCardText(private val texts: CardTextRepository) {

    suspend operator fun invoke(cardId: Uuid, lang: String) {
        canonicalLanguageTag(lang)?.let { texts.delete(cardId, it) }
    }
}
