package app.larova.feature.home

import androidx.compose.runtime.Composable
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.Step
import app.larova.core.domain.model.tableOf
import app.larova.core.domain.usecase.CardDraft
import app.larova.core.domain.usecase.TemplateId
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.template_bedtime
import app.larova.core.ui.resources.template_bedtime_step_1
import app.larova.core.ui.resources.template_bedtime_step_2
import app.larova.core.ui.resources.template_bedtime_step_3
import app.larova.core.ui.resources.template_bedtime_step_4
import app.larova.core.ui.resources.template_bedtime_step_5
import app.larova.core.ui.resources.template_contacts
import app.larova.core.ui.resources.template_contacts_name
import app.larova.core.ui.resources.template_contacts_relation
import app.larova.core.ui.resources.template_contacts_todo
import app.larova.core.ui.resources.template_day
import app.larova.core.ui.resources.template_day_column_1
import app.larova.core.ui.resources.template_day_column_2
import app.larova.core.ui.resources.template_day_row_1_1
import app.larova.core.ui.resources.template_day_row_1_2
import app.larova.core.ui.resources.template_day_row_2_1
import app.larova.core.ui.resources.template_day_row_2_2
import app.larova.core.ui.resources.template_day_row_3_1
import app.larova.core.ui.resources.template_day_row_3_2
import app.larova.core.ui.resources.template_day_row_4_1
import app.larova.core.ui.resources.template_day_row_4_2
import app.larova.core.ui.resources.template_evening
import app.larova.core.ui.resources.template_evening_item_1
import app.larova.core.ui.resources.template_evening_item_2
import app.larova.core.ui.resources.template_evening_item_3
import app.larova.core.ui.resources.template_evening_item_4
import app.larova.core.ui.resources.template_evening_item_5
import app.larova.core.ui.resources.template_food
import app.larova.core.ui.resources.template_food_column_1
import app.larova.core.ui.resources.template_food_column_2
import app.larova.core.ui.resources.template_food_row_1_1
import app.larova.core.ui.resources.template_food_row_1_2
import app.larova.core.ui.resources.template_food_row_2_1
import app.larova.core.ui.resources.template_food_row_2_2
import app.larova.core.ui.resources.template_food_row_3_1
import app.larova.core.ui.resources.template_food_row_3_2
import app.larova.core.ui.resources.template_what_helps
import app.larova.core.ui.resources.template_what_helps_step_1
import app.larova.core.ui.resources.template_what_helps_step_2
import app.larova.core.ui.resources.template_what_helps_step_3
import app.larova.core.ui.theme.TileColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What each template actually puts on the grid.
 *
 * Composable, because every word of it is a string resource: a template is written in whatever
 * language the app is in at the moment it is used, and then it belongs to the parents
 * (`docs/localization.md` §4). Resolving the words here and handing the domain a finished draft is
 * what keeps that true — the alternative, resource identifiers travelling into the database, would
 * mean a tile that silently rewrote itself when somebody changed the app language.
 *
 * One tile each. A template is a starting point, not a starter kit: six tiles landing at once would
 * be a grid somebody has to clear before they can think, and the tile they picked is the one that
 * shows what a guide, a list, a table or a number looks like when it is filled in.
 *
 * The colours and symbols are chosen here and are the only part not translated — they are keys, as
 * every tile's are.
 */
@Composable
internal fun templateDraft(id: TemplateId): CardDraft = when (id) {
    TemplateId.BEDTIME -> CardDraft(
        title = stringResource(Res.string.template_bedtime),
        colorToken = TileColor.SAGE.key,
        icon = TileSymbol.MOON.key,
        payload = CardPayload.Guide(
            steps = stepsOf(
                Res.string.template_bedtime_step_1,
                Res.string.template_bedtime_step_2,
                Res.string.template_bedtime_step_3,
                Res.string.template_bedtime_step_4,
                Res.string.template_bedtime_step_5,
            ),
        ),
    )

    TemplateId.EVENING -> CardDraft(
        title = stringResource(Res.string.template_evening),
        colorToken = TileColor.SKY.key,
        icon = TileSymbol.LIST.key,
        payload = CardPayload.Checklist(
            items = listOf(
                Res.string.template_evening_item_1,
                Res.string.template_evening_item_2,
                Res.string.template_evening_item_3,
                Res.string.template_evening_item_4,
                Res.string.template_evening_item_5,
            ).map { CheckItem(text = stringResource(it)) },
        ),
    )

    /**
     * The number is left empty on purpose. Every other field can carry example text somebody
     * overwrites; a phone number cannot, because a plausible-looking one belongs to a real person
     * and a caregiver would eventually call it. The second line says what is missing instead.
     */
    TemplateId.CONTACTS -> CardDraft(
        title = stringResource(Res.string.template_contacts),
        subtitle = stringResource(Res.string.template_contacts_todo),
        colorToken = TileColor.CLAY.key,
        icon = TileSymbol.PHONE.key,
        payload = CardPayload.Phone(
            displayName = stringResource(Res.string.template_contacts_name),
            number = "",
            relation = stringResource(Res.string.template_contacts_relation),
            inHelpSheet = true,
        ),
    )

    TemplateId.FOOD -> CardDraft(
        title = stringResource(Res.string.template_food),
        colorToken = TileColor.MOSS.key,
        icon = TileSymbol.MEAL.key,
        payload = tableOf(
            columns = listOf(
                stringResource(Res.string.template_food_column_1),
                stringResource(Res.string.template_food_column_2),
            ),
            rows = listOf(
                listOf(
                    stringResource(Res.string.template_food_row_1_1),
                    stringResource(Res.string.template_food_row_1_2),
                ),
                listOf(
                    stringResource(Res.string.template_food_row_2_1),
                    stringResource(Res.string.template_food_row_2_2),
                ),
                listOf(
                    stringResource(Res.string.template_food_row_3_1),
                    stringResource(Res.string.template_food_row_3_2),
                ),
            ),
        ),
    )

    TemplateId.DAY -> CardDraft(
        title = stringResource(Res.string.template_day),
        colorToken = TileColor.SAND.key,
        icon = TileSymbol.CLOCK.key,
        payload = tableOf(
            columns = listOf(
                stringResource(Res.string.template_day_column_1),
                stringResource(Res.string.template_day_column_2),
            ),
            rows = listOf(
                listOf(
                    stringResource(Res.string.template_day_row_1_1),
                    stringResource(Res.string.template_day_row_1_2),
                ),
                listOf(
                    stringResource(Res.string.template_day_row_2_1),
                    stringResource(Res.string.template_day_row_2_2),
                ),
                listOf(
                    stringResource(Res.string.template_day_row_3_1),
                    stringResource(Res.string.template_day_row_3_2),
                ),
                listOf(
                    stringResource(Res.string.template_day_row_4_1),
                    stringResource(Res.string.template_day_row_4_2),
                ),
            ),
        ),
    )

    TemplateId.WHAT_HELPS -> CardDraft(
        title = stringResource(Res.string.template_what_helps),
        colorToken = TileColor.LILAC.key,
        icon = TileSymbol.HEART.key,
        payload = CardPayload.Guide(
            steps = stepsOf(
                Res.string.template_what_helps_step_1,
                Res.string.template_what_helps_step_2,
                Res.string.template_what_helps_step_3,
            ),
        ),
    )
}

@Composable
private fun stepsOf(vararg lines: StringResource): List<Step> =
    lines.map { Step(text = stringResource(it)) }

/** The name shown on the button. The same string the tile is titled with, so the two agree. */
@Composable
internal fun TemplateId.label(): String = stringResource(
    when (this) {
        TemplateId.BEDTIME -> Res.string.template_bedtime
        TemplateId.EVENING -> Res.string.template_evening
        TemplateId.CONTACTS -> Res.string.template_contacts
        TemplateId.FOOD -> Res.string.template_food
        TemplateId.DAY -> Res.string.template_day
        TemplateId.WHAT_HELPS -> Res.string.template_what_helps
    },
)
