package app.larova.feature.card.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.domain.app.LanguageOption
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.edit_cancel
import app.larova.core.ui.resources.edit_language_more
import app.larova.core.ui.resources.edit_language_pick
import app.larova.core.ui.resources.edit_language_search
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Which language a tile is written in, or is being written in.
 *
 * A dialog rather than a destination of its own, for the reason [AppPickerDialog] is one: the graph
 * is two levels deep on purpose, and choosing a language is a step inside making one tile rather
 * than a place somebody navigates to.
 *
 * **Two lists, and the second is the point.** The shortlist is the fourteen Larova itself speaks,
 * because those are the ones most families will want and a list of two hundred is a list nobody
 * reads. But a tile is not limited to them and never was: nothing in `Card.locale`, in `CardText`
 * or in `resolveCardText` cares whether Larova was translated into a language, and a family with a
 * Romanian carer needs a Romanian tile whether or not the buttons around it are Romanian. So the
 * rest of the phone's languages are one tap further on, behind a search — which is what makes two
 * hundred entries usable rather than merely present.
 *
 * Languages already on this tile are filtered out before they get here. Offering one twice would
 * produce a second row under the same `(cardId, lang)` key and quietly replace the first, and a
 * parent who tapped "Türkçe" expecting a blank form would find their own earlier work.
 */
@Composable
fun LanguagePickerDialog(
    /** The likely few, shown first. */
    shortlist: List<LanguageOption>,
    /** Every language this phone can name, shown behind the search. */
    all: List<LanguageOption>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_language_pick)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (searching) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(Res.string.edit_language_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val matches = all.filter { it.matches(query) }
                    LazyColumn {
                        items(matches, key = { it.tag }) { option ->
                            LanguageRow(label = option.endonym, onClick = { onPick(option.tag) })
                        }
                    }
                } else {
                    LazyColumn {
                        items(shortlist, key = { it.tag }) { option ->
                            LanguageRow(label = option.endonym, onClick = { onPick(option.tag) })
                        }
                        item {
                            LanguageRow(
                                label = stringResource(Res.string.edit_language_more),
                                onClick = { searching = true },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.edit_cancel))
            }
        },
    )
}

@Composable
private fun LanguageRow(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget),
    ) {
        Text(text = label, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Matches from either end.
 *
 * A German parent looking for Romanian is as likely to type "Rumänisch" as "Română", and a list
 * that only matched the endonym would answer the first with nothing — so the language's name in
 * the app's own language matches too. The tag is in as well, which costs nothing and helps anybody
 * who knows the code.
 *
 * `lowercase()` with no argument is locale-independent, which is the point rather than an
 * oversight: `docs/localization.md` §3 warns off the locale default because a Turkish `i` folds to
 * `İ`, and a Turkish phone would then fail to find "Italiano".
 */
private fun LanguageOption.matches(query: String): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    return endonym.lowercase().contains(needle) ||
        localName.lowercase().contains(needle) ||
        tag.lowercase().startsWith(needle)
}

/**
 * The languages Larova itself speaks, as the picker's shortlist.
 *
 * The same fourteen as `locales_config.xml` and the `values-*` folders, and deliberately a separate
 * list rather than one derived from them: Compose resources cannot be enumerated at runtime. If a
 * fifteenth language is ever added, `docs/localization.md` §6 lists the other places to touch —
 * add it here too.
 *
 * It is a **shortlist and nothing more**. A tile may be written in any language this phone can
 * name; this only decides which few are offered without searching for them.
 *
 * Primary subtags, which is why `pt` appears here where the app's resource folder is `values-pt-rPT`
 * and the store folder is `pt-PT`. Those two are about which *chrome* a phone gets. This is about
 * what a parent typed, `resolveCardText` matches tile languages on the primary subtag, and asking
 * somebody to choose between two Portugueses to write one tile is a question with no right answer.
 */
internal val APP_LANGUAGES = setOf(
    "en", "de", "fr", "it", "es", "pt", "uk", "pl", "ru", "tr", "ar", "hi", "zh", "ja",
)

/**
 * The language half of a tag, for comparing one against another.
 *
 * A tile written before this picker offered plain tags may carry `pt-PT`, and the phone's list
 * offers `pt`: without this they are two languages, and the picker would offer a language the tile
 * already has.
 */
internal fun String.primaryLanguageSubtag(): String = substringBefore('-').lowercase()
