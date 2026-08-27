package app.larova.feature.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.CardPayload
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.app_missing
import app.larova.core.ui.resources.app_open
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.GuideStepStyle
import org.jetbrains.compose.resources.stringResource

/**
 * A tile that opens another app.
 *
 * The label the parents chose is the heading, at reading size, because it is the only part a
 * caregiver will recognise — "Music for the car" tells them what this is for in a way the app's own
 * name may not.
 *
 * [isInstalled] is asked when the tile is drawn, not remembered from when it was made. An app that
 * has been uninstalled since is an ordinary thing to find, and saying so is better than a button
 * that swallows the tap and leaves somebody pressing it again.
 */
@Composable
fun AppLinkView(
    appLink: CardPayload.AppLink,
    isInstalled: Boolean,
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = appLink.label,
            style = GuideStepStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (isInstalled) {
            Button(
                onClick = { onOpenApp(appLink.packageName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.app_open))
            }
        } else {
            Text(
                text = stringResource(Res.string.app_missing),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
