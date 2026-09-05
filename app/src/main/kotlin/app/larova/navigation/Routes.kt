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
 *
 * [boardId] is the folder a new tile is being made inside; empty is the start screen. It is only
 * ever read for a new tile — an existing one keeps the board it is already on, because saving a
 * tile is not a way to move it.
 */
@Serializable
data class CardEditRoute(val cardId: String = "", val boardId: String = "")

/**
 * One tile in one other language.
 *
 * Both halves come from the tile it was opened from and neither means anything alone. A separate
 * destination rather than a mode on [CardEditRoute]: a translation edits words and nothing else,
 * and folding it into a form with two dozen fields would mean a mode changing what every one of
 * them means.
 */
@Serializable
data class CardTranslationRoute(val cardId: String, val lang: String)

/** [boardId] is the folder being rearranged. Empty is the start screen. */
@Serializable
data class ArrangeRoute(val boardId: String = "")

@Serializable
data object LogRoute

@Serializable
data object HelpRoute

@Serializable
data object TransferRoute

@Serializable
data object SettingsRoute

@Serializable
data object UnlockRoute

/**
 * Choosing a tile's symbol, on its own screen.
 *
 * Carries what is chosen now and the tile's colour, so the grid can show the selection the way it
 * will look. The answer travels back the other way through the editor's `SavedStateHandle` — see
 * `SYMBOL_RESULT` — because the editor owns the half-finished tile and must not be rebuilt.
 */
@Serializable
data class SymbolPickerRoute(val selectedKey: String, val colorToken: String)

/** The key the picker leaves on the editor's back stack entry. */
const val SYMBOL_RESULT = "app.larova.symbolKey"

@Serializable
data object PinSetupRoute
