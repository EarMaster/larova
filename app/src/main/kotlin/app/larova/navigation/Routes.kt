package app.larova.navigation

import kotlinx.serialization.Serializable

/**
 * The navigation graph, as types rather than strings.
 *
 * Deliberately flat: the start screen, a tile, and the three things behind the menu. Two levels at
 * most, because someone searching under pressure should not have to navigate (docs/concept.md
 * §4.1). If this list ever needs a third level, that is a signal about the product, not about the
 * navigation library.
 */
@Serializable
data object HomeRoute

@Serializable
data class CardRoute(val cardId: String)

/**
 * An empty [cardId] means a new tile. One route for both cases rather than two: the editor is the
 * same form either way, and a separate "create" destination would duplicate every field.
 */
@Serializable
data class CardEditRoute(val cardId: String = "")

@Serializable
data object HelpRoute

@Serializable
data object TransferRoute

@Serializable
data object SettingsRoute
