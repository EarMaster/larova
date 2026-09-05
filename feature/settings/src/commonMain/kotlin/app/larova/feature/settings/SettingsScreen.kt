package app.larova.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.Entitlement
import app.larova.core.ui.component.ActionCard
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.icon.Lock
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.Transfer
import app.larova.core.ui.icon.Translate
import app.larova.core.ui.icon.image
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.purchase_body
import app.larova.core.ui.resources.purchase_body_support
import app.larova.core.ui.resources.purchase_buy
import app.larova.core.ui.resources.purchase_buy_price
import app.larova.core.ui.resources.purchase_later
import app.larova.core.ui.resources.purchase_pending
import app.larova.core.ui.resources.purchase_title
import app.larova.core.ui.resources.purchase_unavailable
import app.larova.core.ui.resources.settings_appearance
import app.larova.core.ui.resources.settings_appearance_dark
import app.larova.core.ui.resources.settings_appearance_dark_hint
import app.larova.core.ui.resources.settings_appearance_light
import app.larova.core.ui.resources.settings_appearance_light_hint
import app.larova.core.ui.resources.settings_appearance_night
import app.larova.core.ui.resources.settings_appearance_night_hint
import app.larova.core.ui.resources.settings_appearance_system
import app.larova.core.ui.resources.settings_appearance_system_hint
import app.larova.core.ui.resources.settings_language
import app.larova.core.ui.resources.settings_language_hint
import app.larova.core.ui.resources.settings_support
import app.larova.core.ui.resources.settings_support_count
import app.larova.core.ui.resources.settings_support_hint
import app.larova.core.ui.resources.settings_support_thanks
import app.larova.core.ui.resources.settings_support_unavailable
import app.larova.core.ui.resources.settings_title
import app.larova.core.ui.resources.settings_transfer_hint
import app.larova.core.ui.resources.settings_unlock
import app.larova.core.ui.resources.settings_unlock_build
import app.larova.core.ui.resources.settings_unlock_checking
import app.larova.core.ui.resources.settings_unlock_hint
import app.larova.core.ui.resources.settings_unlock_missing
import app.larova.core.ui.resources.settings_unlock_none
import app.larova.core.ui.resources.settings_unlock_owned
import app.larova.core.ui.resources.settings_version
import app.larova.core.ui.resources.transfer_title
import app.larova.core.ui.resources.view_leave
import app.larova.core.ui.resources.view_locked_note
import app.larova.core.ui.resources.view_parent_active
import app.larova.core.ui.resources.view_pin_change
import app.larova.core.ui.resources.view_unlock_title
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.TileColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Where a build with nothing to sell points instead.
 *
 * Liberapay rather than GitHub Sponsors, for the reasons in `.github/FUNDING.yml`: no cut, a
 * non-profit, and no US tax paperwork for a German maintainer. The same address the GitHub Release
 * notes carry, and it appears **only** in a build with no paid tier — in a Play build the card
 * above asks Play again instead, because an appeal to donate beside a paid product is asking twice.
 *
 * Here rather than in `:app` because this is the screen that writes it out: the string shows the
 * address and the card opens it, and two copies of a URL is one copy too many.
 */
const val SUPPORT_URL: String = "https://liberapay.com/EarMaster"

/**
 * Settings.
 *
 * Appearance, the way in and out of parent view, and — once you are in it — the parent-view work
 * that is not editing a tile. Appearance comes first among the preferences because night mode is
 * not a preference in the usual sense: it exists for the leading use case, reading a guide aloud
 * in a darkened bedroom, and it is the one setting a caregiver might reasonably reach for
 * themselves.
 *
 * No help bar here. This is a screen somebody opened on purpose to change something, not one they
 * are on while a child is upset, and the red bar means "now" everywhere else in the product.
 */
@Composable
fun SettingsScreen(
    appearance: AppearanceSetting,
    onAppearanceChange: (AppearanceSetting) -> Unit,
    isParentView: Boolean,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onChangePin: () -> Unit,
    onOpenTransfer: () -> Unit,
    onBack: () -> Unit,
    entitlement: Entitlement,
    /**
     * Asks the store again. Null in a build with no store behind it, where there is nothing to
     * ask and the status line says so instead.
     */
    onCheckPurchases: (() -> Unit)?,
    /**
     * How the last look at the store went: what the card says while it is asking, and — when the
     * answer was nothing — the offer that follows.
     */
    unlockCheck: UnlockCheck,
    /** Closes that offer without buying. */
    onDismissUnlockCheck: () -> Unit,
    /**
     * Opens Play's sheet for the full version, from the offer above. Null in a build with nothing
     * for sale, where nothing is locked and the offer is unreachable anyway.
     */
    onBuyUnlock: (() -> Unit)?,
    /**
     * Hands [SUPPORT_URL] to the browser. Reached by tapping the full-version card in a build with
     * no store behind it, which is the only build that shows the address at all.
     */
    onOpenSupportPage: () -> Unit,
    /**
     * Opens Android's own language screen for Larova. Null on a phone with no such screen — below
     * Android 13 the app's language is the phone's, and there is nothing to send anybody to.
     */
    onOpenLanguageSettings: (() -> Unit)?,
    supportCount: Int,
    /** Opens Play's sheet for the contribution. Null in a build with no store behind it. */
    onSupport: (() -> Unit)?,
    supportMessage: SupportMessage?,
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.settings_title),
        onHelp = null,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Above appearance, because it is the reason most people open this screen: they
            // came here to change something and found they could not.
            ViewModeSection(
                isParentView = isParentView,
                onUnlock = onUnlock,
                onLock = onLock,
                onChangePin = onChangePin,
            )

            // Not behind parent view, and that is the whole point of it: the person who needs
            // another language is the caregiver holding the phone, and they cannot be asked for a
            // PIN to read the app in a language they understand. It is also why this is a handover
            // rather than a picker — Android's own screen does the list, the search and the
            // restart, and knows about `locales_config.xml` without being told twice.
            if (onOpenLanguageSettings != null) {
                ActionCard(
                    icon = Translate,
                    title = stringResource(Res.string.settings_language),
                    description = stringResource(Res.string.settings_language_hint),
                    onClick = onOpenLanguageSettings,
                    // Sage: not sand, sky or rose, which are the three cards below it.
                    token = TileColor.SAGE,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            // Backup used to live behind the start screen's overflow menu, which is the last place
            // somebody looks for it — a menu is where things go when nobody has decided where they
            // belong. It belongs here: it is parent-view work, and this is the screen parent view
            // is turned on from.
            if (isParentView) {
                ActionCard(
                    icon = Transfer,
                    title = stringResource(Res.string.transfer_title),
                    description = stringResource(Res.string.settings_transfer_hint),
                    onClick = onOpenTransfer,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                // Parent view only: buying and checking a purchase is parent-view work, and a
                // caregiver has no use for either.
                //
                // A card like the two around it, and tappable as a whole rather than carrying a
                // button of its own. The status matters more than the tap: restoring already
                // happens at every launch, so somebody who paid on another phone is usually
                // unlocked before they think to look — what they lack is a way to *see* that.
                // The retry is for the one case the automatic attempt cannot cover, a phone that
                // was offline at start and is online now, and the description says so.
                ActionCard(
                    // A padlock while it is shut, a key once it is not. A lock beside the word
                    // "Unlocked" contradicts the only line anybody reads.
                    icon = if (entitlement == Entitlement.NONE) Lock else TileSymbol.KEY.image,
                    title = stringResource(Res.string.settings_unlock),
                    // While the store is being asked, the status says so. That wait is the whole
                    // reason the card looked broken: the question goes to another app, which may
                    // have to be woken up to answer it, and nothing here moved meanwhile.
                    status = if (unlockCheck is UnlockCheck.Checking) {
                        stringResource(Res.string.settings_unlock_checking)
                    } else {
                        when (entitlement) {
                            Entitlement.NONE -> stringResource(Res.string.settings_unlock_none)
                            Entitlement.PLAY,
                            Entitlement.KEY,
                            Entitlement.BUILD,
                            -> stringResource(Res.string.settings_unlock_owned)
                        }
                    },
                    // Keyed on the store rather than on the entitlement: with nothing to ask,
                    // the honest sentence is why everything is available, not how to restore it.
                    // Somebody who compiled Larova themselves would otherwise be left wondering.
                    description = if (onCheckPurchases != null) {
                        stringResource(Res.string.settings_unlock_hint)
                    } else {
                        stringResource(Res.string.settings_unlock_build, SUPPORT_URL)
                    },
                    // Two builds, two honest actions for one card. With a store behind it the tap
                    // asks Play again; with nothing for sale there is nothing to ask, and the card
                    // is the only place the support address appears — so the tap opens it. It used
                    // to be disabled there, which left an address on screen and no way to reach it.
                    onClick = onCheckPurchases ?: onOpenSupportPage,
                    // Not while it is already asking: a second tap would start a second question,
                    // and the card going quiet under the finger is the other half of saying that
                    // the first one is still open.
                    enabled = unlockCheck !is UnlockCheck.Checking,
                    // Sky: not sand, which is the backup card, and not rose, which is the
                    // contribution. Three blocks, three colours, so they read as three things.
                    token = TileColor.SKY,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                // The answer to that tap, when the answer is nothing. Not a line on the card:
                // "still not unlocked" under a card that already reads "Not unlocked" is not an
                // answer anybody would notice, and the useful next step — buying it — has
                // nowhere to go there.
                if (unlockCheck is UnlockCheck.NotFound) {
                    UnlockNotFoundDialog(
                        found = unlockCheck,
                        onBuy = onBuyUnlock,
                        onDismiss = onDismissUnlockCheck,
                    )
                }

                // Below the unlock, and visually a sibling of the backup card rather than of it:
                // this buys nothing and must not read as a second paid tier.
                if (onSupport != null) {
                    ActionCard(
                        icon = TileSymbol.HEART.image,
                        title = stringResource(Res.string.settings_support),
                        description = supportDescription(supportCount, supportMessage),
                        onClick = onSupport,
                        // Rose rather than sand: warm, and not the colour of anything functional.
                        token = TileColor.ROSE,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            Text(
                text = stringResource(Res.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            AppearanceOptions(
                selected = appearance,
                onSelect = onAppearanceChange,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Not hidden behind parent view. It is the first thing any support message needs and
            // the last thing anybody would think to ask a caregiver to find.
            Text(
                text = stringResource(Res.string.settings_version, appVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * What a tap on the full-version card found, when it found nothing.
 *
 * The same offer the locked tile types make, in the one place somebody goes looking for it
 * deliberately rather than running into it. Why nothing was unlocked comes first — the two
 * reasons that actually happen, a different Google account and a store that could not be reached
 * — and only then what buying would be: somebody who came here to restore a purchase is not
 * asking to be sold one.
 *
 * The text scrolls. Three paragraphs at the 200 % font scale this app promises are taller than a
 * dialog, and Material does not scroll that slot on its own.
 */
@Composable
private fun UnlockNotFoundDialog(
    found: UnlockCheck.NotFound,
    onBuy: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // The same padlock the locked tiles carry, so the two read as one thing. No description:
        // the title beside it says what it is.
        icon = { Icon(imageVector = Lock, contentDescription = null) },
        title = { Text(stringResource(Res.string.purchase_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_unlock_missing),
                    style = MaterialTheme.typography.bodyLarge,
                )
                // Two paragraphs, because they answer different questions: what the payment is,
                // then who it goes to. The same pair the locked tile shows, in the same order.
                Text(
                    text = stringResource(Res.string.purchase_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(Res.string.purchase_body_support),
                    style = MaterialTheme.typography.bodyMedium,
                )
                found.message?.let {
                    Text(
                        text = when (it) {
                            UnlockMessage.PENDING -> stringResource(Res.string.purchase_pending)
                            UnlockMessage.UNAVAILABLE ->
                                stringResource(Res.string.purchase_unavailable)
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onBuy?.invoke() },
                // Disabled rather than absent, for the same reason as on the locked tile: a button
                // that silently does nothing is worse than one that says it cannot.
                enabled = onBuy != null,
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(
                    // Play's own price when the store answered, the plain label when it did not.
                    // Never a number written here: only Play prices for a country.
                    text = found.price
                        ?.let { stringResource(Res.string.purchase_buy_price, it) }
                        ?: stringResource(Res.string.purchase_buy),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.purchase_later))
            }
        },
    )
}

/** What the contribution card says under its title, which changes as things happen. */
enum class SupportMessage {
    /** A contribution just went through. The count has already gone up beside it. */
    THANKS,

    /** Play could not be reached, or refused. Worth saying; not worth a dialog. */
    UNAVAILABLE,
}

/**
 * The line under the contribution card.
 *
 * The count is the feedback that matters — "supported once" becoming "supported twice" is what
 * tells somebody their tap did something, better than any toast would. The outcome message takes
 * precedence only while there is one, and the explanation shows until anybody has given at all.
 */
@Composable
private fun supportDescription(count: Int, message: SupportMessage?): String = when {
    message == SupportMessage.UNAVAILABLE -> stringResource(Res.string.settings_support_unavailable)
    message == SupportMessage.THANKS -> stringResource(Res.string.settings_support_thanks)
    count > 0 -> pluralStringResource(Res.plurals.settings_support_count, count, count)
    else -> stringResource(Res.string.settings_support_hint)
}

/**
 * The four appearances, as one thing.
 *
 * Loose rows on the page read as four separate settings that happen to be near each other, and a
 * caregiver who reads them that way looks for the "off". One surface with a line between each row
 * says what a radio group means — pick exactly one of these — before anybody has read a word of
 * it. `selectableGroup` says the same to a screen reader, which otherwise announces four unrelated
 * radio buttons rather than "1 of 4".
 */
@Composable
private fun AppearanceOptions(
    selected: AppearanceSetting,
    onSelect: (AppearanceSetting) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.TileRadius),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            AppearanceSetting.entries.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                AppearanceOption(
                    label = stringResource(option.label),
                    hint = stringResource(option.hint),
                    selected = option == selected,
                    onSelect = { onSelect(option) },
                )
            }
        }
    }
}

/**
 * A row, not a switch. Four states cannot be a toggle, and a 56dp selectable row is easier to hit
 * than a radio button on its own — the whole row is the target, which is what
 * `Modifier.selectable` on the row rather than on the control gives.
 *
 * The padding is inside `selectable` rather than outside it, so the target is the full width of
 * the card and not a strip down the middle of it.
 */
@Composable
private fun AppearanceOption(
    label: String,
    hint: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // null: the row carries the semantics, so a screen reader announces one control rather
        // than two — and the hint is then read as part of that control's own label, which is what
        // makes it an explanation of this option rather than a stray sentence.
        RadioButton(selected = selected, onClick = null)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val AppearanceSetting.label: StringResource
    get() = when (this) {
        AppearanceSetting.SYSTEM -> Res.string.settings_appearance_system
        AppearanceSetting.LIGHT -> Res.string.settings_appearance_light
        AppearanceSetting.DARK -> Res.string.settings_appearance_dark
        AppearanceSetting.NIGHT -> Res.string.settings_appearance_night
    }

/**
 * Every option has one, and they are deliberately parallel: three say what the screen will look
 * like, and night says what it is *for*, because that is the one whose name does not carry it.
 * Describing only night left the reasonable question of what was wrong with the other three.
 */
private val AppearanceSetting.hint: StringResource
    get() = when (this) {
        AppearanceSetting.SYSTEM -> Res.string.settings_appearance_system_hint
        AppearanceSetting.LIGHT -> Res.string.settings_appearance_light_hint
        AppearanceSetting.DARK -> Res.string.settings_appearance_dark_hint
        AppearanceSetting.NIGHT -> Res.string.settings_appearance_night_hint
    }

/**
 * Which view the app is in, and the way out of it.
 *
 * Stated in words rather than shown as a switch. A toggle labelled "parent view" invites a
 * caregiver to try it, and the honest answer to that tap — a PIN prompt — is better reached from a
 * button that says what it will do.
 */
@Composable
private fun ViewModeSection(
    isParentView: Boolean,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onChangePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(
                if (isParentView) Res.string.view_parent_active else Res.string.view_locked_note,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isParentView) {
            OutlinedButton(
                onClick = onLock,
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.view_leave))
            }
            OutlinedButton(
                onClick = onChangePin,
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.view_pin_change))
            }
        } else {
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.view_unlock_title))
            }
        }
    }
}
