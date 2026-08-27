package app.larova.feature.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.CardPayload
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.help_call_note
import app.larova.core.ui.resources.home_empty_title
import app.larova.core.ui.resources.tile_call
import app.larova.core.ui.resources.tile_link
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.GuideStepStyle
import org.jetbrains.compose.resources.stringResource

/**
 * A note: text, at a size someone can read across a kitchen table.
 *
 * Rendered as written, with no formatting applied. What a parent typed about their child is not
 * something to reinterpret — the app stores and displays, it does not parse.
 */
@Composable
fun NoteView(note: CardPayload.Note, modifier: Modifier = Modifier) {
    if (note.text.isBlank()) {
        EmptyPayloadNote(modifier = modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
    ) {
        Text(
            text = note.text,
            style = GuideStepStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * A person and their number.
 *
 * The button prepares the call; it does not place it. The note underneath says so in as many words,
 * because someone who has never used this app should not have to find out by pressing it.
 */
@Composable
fun CallView(
    phone: CardPayload.Phone,
    onPrepareCall: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = phone.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        val relation = phone.relation
        if (!relation.isNullOrBlank()) {
            Text(
                text = relation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            // Never mirrored in a right-to-left layout: a phone number reads left to right in
            // every language, and reversing one produces a different number.
            text = phone.number,
            style = GuideStepStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Button(
            onClick = { onPrepareCall(phone.number) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget),
        ) {
            Text(stringResource(Res.string.tile_call))
        }

        Text(
            text = stringResource(Res.string.help_call_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A link, with the address in plain sight so nobody has to guess where the button goes. */
@Composable
fun WebsiteView(
    web: CardPayload.Web,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val label = web.label
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = web.url,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { onOpenUrl(web.url) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget),
        ) {
            Text(stringResource(Res.string.tile_link))
        }
    }
}

/**
 * A tile that exists but has nothing in it yet. Reachable while a parent is still filling it in,
 * and better than a blank screen that looks broken.
 */
@Composable
internal fun EmptyPayloadNote(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
