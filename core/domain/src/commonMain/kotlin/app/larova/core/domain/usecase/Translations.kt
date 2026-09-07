package app.larova.core.domain.usecase

import app.larova.core.domain.app.AppLanguage
import app.larova.core.domain.app.LanguageOption
import app.larova.core.domain.app.Translators
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.canonicalLanguageTag
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.CardTextRepository
import app.larova.core.domain.repository.PreferencesRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Whether the tile screen should offer to translate at all.
 *
 * Asked when a tile is drawn rather than once at startup, for the same reason `IsAppInstalled` is:
 * an app that was there in the morning and gone by the evening is an ordinary thing to happen to a
 * phone, and the answer is cheap.
 */
class CanTranslate(private val translators: Translators) {

    suspend operator fun invoke(): Boolean = translators.canTranslate()
}

/** Every variant of one tile, so a screen can say which languages it exists in. */
@OptIn(ExperimentalUuidApi::class)
class ObserveCardText(private val texts: CardTextRepository) {
    operator fun invoke(cardId: Uuid): Flow<List<CardText>> = texts.observeForCard(cardId)
}

/** Every variant there is, for the grid, which resolves a whole board at once. */
class ObserveAllCardText(private val texts: CardTextRepository) {
    operator fun invoke(): Flow<List<CardText>> = texts.observeAll()
}

/**
 * What the languages on this phone are, for the setting that picks one.
 *
 * [languages] is every language some tile can be read in, which is **both** halves: the languages
 * tiles were written in and the languages they were translated into. Built from the tiles rather
 * than from a list of what the app supports, because the only languages worth offering are the ones
 * something is actually written in — and that set changes as a parent adds them.
 *
 * Leaving the written half out was a bug with an obvious shape once seen: a family whose tiles are
 * German with an Italian translation was offered Italian and English and not German, so the one
 * language the tiles were actually in could not be asked for by name.
 *
 * [hasTranslations] is what decides whether the setting exists at all. A phone where nothing has
 * been translated has one language and no choice to make, and a picker offering it is furniture.
 */
data class TileLanguages(
    val languages: List<String> = emptyList(),
    val hasTranslations: Boolean = false,
)

class ObserveTileLanguages(
    private val cards: CardRepository,
    private val texts: CardTextRepository,
) {
    operator fun invoke(): Flow<TileLanguages> =
        combine(cards.observeAllCards(), texts.observeAll()) { allCards, allTexts ->
            TileLanguages(
                languages = (allCards.mapNotNull { it.locale } + allTexts.map { it.lang })
                    .distinct()
                    .sorted(),
                hasTranslations = allTexts.isNotEmpty(),
            )
        }
}

/**
 * Which language tiles should be shown in, resolved for use.
 *
 * The stored preference is nullable and means "follow the app"; this turns that into the tag a
 * caller can hand to `resolveCardText` without every caller repeating the fallback. One place, so
 * the grid and the tile screen cannot disagree about what "follow the app" resolved to.
 */
class ContentLanguage(
    private val preferences: PreferencesRepository,
    private val appLanguage: AppLanguage,
    private val tileLanguages: ObserveTileLanguages,
) {

    /** The chosen tag, or the app's own when nothing has been chosen. */
    operator fun invoke(): Flow<String> =
        preferences.observeContentLanguage().map { it ?: appLanguage.current }

    /** Null puts it back to following the app, which is where it starts. */
    suspend fun set(tag: String?) = preferences.setContentLanguage(canonicalLanguageTag(tag))

    /** What was actually chosen, as opposed to what it resolved to. Null is "follow the app". */
    fun chosen(): Flow<String?> = preferences.observeContentLanguage()

    fun nameOf(tag: String): String = appLanguage.nameOf(tag)

    /** Every language a tile can be written in, which is not the fourteen the app is written in. */
    fun available(): List<LanguageOption> = appLanguage.available()

    /** Which languages this phone's tiles are actually in — the set the setting picks from. */
    fun onThisPhone(): Flow<TileLanguages> = tileLanguages()
}

/**
 * The translation questions a screen asks, in one place.
 *
 * Grouped the way [Apps] is, and for the same reason: `CardViewModel` is one constructor parameter
 * from the count Detekt refuses, and one holder here is cheaper than a suppression at every screen
 * that grows a language control.
 *
 * Reading and writing both, since the editor learned to switch language in place: the screen that
 * lists a tile's languages is now the screen that writes one, and splitting the two across two
 * holders would put the same feature in two constructor slots.
 */
class Translations(
    private val canTranslate: CanTranslate,
    private val cardText: ObserveCardText,
    private val allCardText: ObserveAllCardText,
    private val contentLanguage: ContentLanguage,
    private val saveCardText: SaveCardText,
    private val deleteCardText: DeleteCardText,
) {

    suspend fun isAvailable(): Boolean = canTranslate()

    @OptIn(ExperimentalUuidApi::class)
    fun textsFor(cardId: Uuid): Flow<List<CardText>> = cardText(cardId)

    fun allTexts(): Flow<List<CardText>> = allCardText()

    fun tileLanguages(): Flow<TileLanguages> = contentLanguage.onThisPhone()

    fun language(): Flow<String> = contentLanguage()

    fun chosenLanguage(): Flow<String?> = contentLanguage.chosen()

    suspend fun choose(tag: String?) = contentLanguage.set(tag)

    fun nameOf(tag: String): String = contentLanguage.nameOf(tag)

    fun available(): List<LanguageOption> = contentLanguage.available()

    @OptIn(ExperimentalUuidApi::class)
    suspend fun save(
        cardId: Uuid,
        lang: String,
        title: String,
        subtitle: String?,
        payload: String,
    ): SaveCardText.Result = saveCardText(cardId, lang, title, subtitle, payload)

    @OptIn(ExperimentalUuidApi::class)
    suspend fun remove(cardId: Uuid, lang: String) = deleteCardText(cardId, lang)
}
