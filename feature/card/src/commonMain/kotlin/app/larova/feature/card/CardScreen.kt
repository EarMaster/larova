package app.larova.feature.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.CardType
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.GuideStepStyle

/**
 * What opening a tile leads to.
 *
 * One screen per tile type arrives with M1; this is the frame they all share. The step style is
 * already wired up because it is the one typographic decision that is not a preference: guide text
 * is read aloud, in dim light, often by someone over 65, so 22sp is a floor.
 */
@Composable
fun CardScreen(
    title: String,
    type: CardType?,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = title,
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = GuideStepStyle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (type != null) {
                Text(
                    text = type.key,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
