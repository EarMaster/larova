package app.larova.core.domain.usecase

import app.larova.core.domain.app.AppLanguage
import app.larova.core.domain.app.Translators
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.canonicalLanguageTag
import app.larova.core.domain.repository.CardTextRepository
import app.larova.core.domain.repository.PreferencesRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
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
 * Which language tiles should be shown in, resolved for use.
 *
 * The stored preference is nullable and means "follow the app"; this turns that into the tag a
 * caller can hand to `resolveCardText` without every caller repeating the fallback. One place, so
 * the grid and the tile screen cannot disagree about what "follow the app" resolved to.
 */
class ContentLanguage(
    private val preferences: PreferencesRepository,
    private val appLanguage: AppLanguage,
) {

    /** The chosen tag, or the app's own when nothing has been chosen. */
    operator fun invoke(): Flow<String> =
        preferences.observeContentLanguage().map { it ?: appLanguage.current }

    /** Null puts it back to following the app, which is where it starts. */
    suspend fun set(tag: String?) = preferences.setContentLanguage(canonicalLanguageTag(tag))

    /** What was actually chosen, as opposed to what it resolved to. Null is "follow the app". */
    fun chosen(): Flow<String?> = preferences.observeContentLanguage()

    fun nameOf(tag: String): String = appLanguage.nameOf(tag)
}

/**
 * The translation questions a screen asks, in one place.
 *
 * Grouped the way [Apps] is, and for the same reason: `CardViewModel` is one constructor parameter
 * from the count Detekt refuses, and one holder here is cheaper than a suppression at every screen
 * that grows a language control.
 */
class Translations(
    private val canTranslate: CanTranslate,
    private val cardText: ObserveCardText,
    private val allCardText: ObserveAllCardText,
    private val contentLanguage: ContentLanguage,
) {

    suspend fun isAvailable(): Boolean = canTranslate()

    @OptIn(ExperimentalUuidApi::class)
    fun textsFor(cardId: Uuid): Flow<List<CardText>> = cardText(cardId)

    fun allTexts(): Flow<List<CardText>> = allCardText()

    fun language(): Flow<String> = contentLanguage()

    fun chosenLanguage(): Flow<String?> = contentLanguage.chosen()

    suspend fun choose(tag: String?) = contentLanguage.set(tag)

    fun nameOf(tag: String): String = contentLanguage.nameOf(tag)
}
