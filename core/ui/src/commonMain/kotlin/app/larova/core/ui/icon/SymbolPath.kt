package app.larova.core.ui.icon

/**
 * One drawn shape of a tile symbol: the `d` attribute of an SVG element, and whether it is filled.
 *
 * The path data crosses from the SVG into Kotlin untouched. Compose's `PathParser` reads the same
 * syntax the file already used, so nothing in this project has to understand a Bézier curve —
 * which is the difference between a fifty-line Gradle task and a geometry library.
 *
 * Written by `:core:ui:generateTileSymbols`; see `core/ui/icons/README.md`.
 */
internal data class SymbolPath(val data: String, val filled: Boolean = false)
