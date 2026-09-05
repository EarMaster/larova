package app.larova.core.domain.usecase

import app.larova.core.domain.app.Translators

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

/**
 * The translation questions a screen asks, in one place.
 *
 * Grouped the way [Apps] is, and for the same reason: `CardViewModel` is one constructor parameter
 * from the count Detekt refuses, and a second holder here is cheaper than a suppression there. It
 * is also where the stored-translation use cases will land, so the screen gains a dependency once
 * rather than each time this feature grows.
 */
class Translations(private val canTranslate: CanTranslate) {

    suspend fun isAvailable(): Boolean = canTranslate()
}
