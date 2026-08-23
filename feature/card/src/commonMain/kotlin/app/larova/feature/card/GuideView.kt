package app.larova.feature.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.CardPayload
import app.larova.core.ui.component.KeepScreenOn
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.guide_finish
import app.larova.core.ui.resources.guide_next
import app.larova.core.ui.resources.guide_previous
import app.larova.core.ui.resources.guide_screen_stays_on
import app.larova.core.ui.resources.guide_step_of
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.GuideStepStyle
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.Signal
import app.larova.core.ui.theme.AppMode
import org.jetbrains.compose.resources.stringResource

/**
 * A guide, one step at a time.
 *
 * One step per screen rather than a scrolling list. The person following it has their hands full
 * and is reading in dim light; "where was I" is the failure this screen exists to prevent, and a
 * list of five paragraphs reintroduces it.
 *
 * The step text is 22sp because it is read aloud, often by someone over 65.
 */
@Composable
fun GuideView(
    guide: CardPayload.Guide,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit = {},
) {
    if (guide.steps.isEmpty()) {
        EmptyPayloadNote(modifier = modifier)
        return
    }

    // The screen stays awake for as long as this view is on it, and only this view.
    KeepScreenOn()

    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = guide.steps[index.coerceIn(guide.steps.indices)]
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepProgress(current = index + 1, total = guide.steps.size)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = step.text,
                style = GuideStepStyle,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Text(
            text = stringResource(Res.string.guide_screen_stays_on),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        StepControls(
            hasPrevious = index > 0,
            isLast = index == guide.steps.lastIndex,
            onPrevious = { if (index > 0) index-- },
            onNext = { if (index < guide.steps.lastIndex) index++ else onFinish() },
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

/**
 * Amber marks the step that is happening now — the one thing amber means anywhere in the product.
 * The count in words is next to it, because a row of dots alone tells a screen reader nothing and
 * tells someone with poor eyesight very little.
 */
@Composable
private fun StepProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    val dark = LocalAppMode.current != AppMode.LIGHT
    val activeColor = if (dark) Signal.amberOnDark else Signal.amberOnLight

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.guide_step_of, current, total),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            // The dots repeat what the line above says, so they are hidden from screen readers
            // rather than announced as "circle, circle, circle".
            modifier = Modifier.clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(total.coerceAtMost(MAX_DOTS)) { i ->
                val isCurrent = i == (current - 1).coerceAtMost(MAX_DOTS - 1)
                Surface(
                    modifier = Modifier.size(if (isCurrent) 10.dp else 7.dp),
                    shape = CircleShape,
                    color = if (isCurrent) activeColor else MaterialTheme.colorScheme.outline,
                    content = {},
                )
            }
        }
    }
}

@Composable
private fun StepControls(
    hasPrevious: Boolean,
    isLast: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (hasPrevious) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.guide_previous))
            }
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .weight(if (hasPrevious) 1f else 2f)
                .heightIn(min = Dimens.MinTouchTarget),
        ) {
            Text(
                stringResource(
                    if (isLast) Res.string.guide_finish else Res.string.guide_next,
                ),
            )
        }
    }
}

private const val MAX_DOTS = 8
