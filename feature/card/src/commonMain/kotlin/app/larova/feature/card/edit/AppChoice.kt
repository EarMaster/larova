package app.larova.feature.card.edit

import androidx.compose.ui.graphics.ImageBitmap

/**
 * One app as the picker draws it.
 *
 * The icon is the live system one, decoded once when the list is loaded. It is shown and never
 * stored: what goes on the tile is a package name and a label the parents can rewrite, plus the
 * symbol key they choose like on any other tile (invariant 1 in `AGENTS.md`).
 */
data class AppChoice(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null,
)
