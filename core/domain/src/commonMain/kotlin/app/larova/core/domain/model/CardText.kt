package app.larova.core.domain.model

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * One tile's text in a language other than the one it was written in.
 *
 * A **whole-tile** variant: title, second line and payload together, never a per-field map. That is
 * the decision the rest of this file exists to protect. A map would allow a tile whose title is
 * Ukrainian and whose third step is still German, and every screen and every payload type would
 * then need its own answer for what to do about the hole. One variant means one fallback rule, in
 * one place — [resolveCardText] — and it means no [CardPayload] type ever learns that translation
 * exists.
 *
 * Larova does not make these. It has no internet permission and never will, and every on-device
 * translation library within reach downloads its models over the network. A variant is text a
 * parent wrote or pasted, the same as the original.
 *
 * **No id.** The key is `(cardId, lang)`, because a variant has no identity of its own — it is one
 * card's text in one language. A surrogate id would let one backup carry two rows claiming the same
 * tile and the same language with nothing to say which of them wins. The cost, accepted from the
 * first release: one variant per language per card, for good.
 */
@OptIn(ExperimentalUuidApi::class)
data class CardText(
    val cardId: Uuid,
    /** A canonical BCP-47 tag — `de`, `pt-PT`, `zh-Hans`. Never a language's name. */
    val lang: String,
    val title: String,
    val subtitle: String? = null,
    /** JSON, encoded exactly as [Card.payload] is, and always the same [CardType] as the card's. */
    val payload: String,
    val updatedAt: Instant,
)

/**
 * What a screen should show for a tile, and why.
 *
 * The "why" is carried rather than re-derived, because three different places would otherwise each
 * work out whether they were looking at a translation and one of them would get it wrong.
 */
data class ResolvedCardText(
    val title: String,
    val subtitle: String?,
    val payload: String,
    /** The variant's tag, or null when this is the tile's own text. */
    val lang: String?,
    /** The tile was edited after this translation was written. Shown anyway — see below. */
    val possiblyOutOfDate: Boolean,
)

/**
 * Which text to show, given what the reader asked for.
 *
 * Stops at the first hit:
 *
 * 1. nothing was asked for, what was asked for is not a language tag, or what was asked for is the
 *    language the original was written in → **the original**. The authored text is never a stale
 *    translation of itself;
 * 2. a variant of this tile whose tag matches exactly;
 * 3. a variant whose primary subtag matches — `pt-PT` asked for, `pt` stored. Ties are broken by
 *    preferring the tag with no region and then the lowest alphabetically, which is **deterministic
 *    and independent of row order and of timestamps**: breaking a tie on `updatedAt` would silently
 *    change which language a caregiver sees because somebody edited an unrelated variant;
 * 4. **the original.**
 *
 * **A tile with no variant for the asked-for language is never hidden and never dimmed.** It falls
 * to step 4 and shows what the parent wrote. There is no fifth rule and no filter anywhere in the
 * app — hiding a tile because it lacks a translation would mean a caregiver reading Ukrainian never
 * seeing the tile about choking, with nothing on screen to say one was withheld. Absent looks
 * exactly like never-existed.
 *
 * The second half of step 1 is unreachable until somebody records what language a tile was written
 * in, so on every card that exists today this is: exact tag, then primary subtag, then the original.
 */
@OptIn(ExperimentalUuidApi::class)
fun resolveCardText(
    card: Card,
    variants: List<CardText>,
    requested: String?,
): ResolvedCardText {
    val wanted = canonicalLanguageTag(requested)
    if (wanted == null || wanted == canonicalLanguageTag(card.locale)) return card.ownText()

    val mine = variants.filter { it.cardId == card.id }
    val chosen = mine.firstOrNull { it.lang == wanted }
        ?: mine
            .filter { it.lang.primarySubtag() == wanted.primarySubtag() }
            // No region first, then alphabetically. Both halves are about giving the same answer
            // every time rather than the cleverest one.
            .minWithOrNull(compareBy({ it.lang.contains('-') }, { it.lang }))

    return chosen?.let { card.showing(it) } ?: card.ownText()
}

private fun Card.ownText() = ResolvedCardText(
    title = title,
    subtitle = subtitle,
    payload = payload,
    lang = null,
    // The original is never out of date with respect to itself.
    possiblyOutOfDate = false,
)

private fun Card.showing(variant: CardText) = ResolvedCardText(
    title = variant.title,
    subtitle = variant.subtitle,
    payload = variant.payload,
    lang = variant.lang,
    // Strictly before, so a restore that writes both with the same timestamp reads as fresh.
    //
    // Shown either way. Showing German to somebody who cannot read German in the name of caution
    // helps nobody; what the flag is for is saying so, and keeping the original one tap away.
    possiblyOutOfDate = variant.updatedAt < updatedAt,
)

private fun String.primarySubtag(): String = substringBefore('-')

/**
 * A language tag in one shape, or null for anything that is not a tag at all.
 *
 * Applied at every write boundary and nowhere else, so that two spellings of Portuguese cannot end
 * up as two languages. Canonicalising on the way in rather than declaring the column
 * `COLLATE NOCASE` is deliberate: Room's schema validator does not compare collations, so a
 * migration whose collation had drifted from the entity's would validate cleanly and then behave
 * differently on an upgraded phone than on a fresh one — which is the class of bug nobody finds.
 *
 * Never guesses and never invents a region: `de` stays `de` rather than becoming `de-DE`.
 */
fun canonicalLanguageTag(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (!TAG.matches(trimmed)) return null
    return trimmed.split('-').mapIndexed { index, part ->
        when {
            index == 0 -> part.lowercaseAscii()
            // A two-letter subtag after the first is a region: `pt-PT`.
            part.length == REGION_LENGTH && part.all { it.isLatinLetter() } -> part.uppercaseAscii()
            // A four-letter one is a script: `zh-Hans`.
            part.length == SCRIPT_LENGTH && part.all { it.isLatinLetter() } ->
                part.take(1).uppercaseAscii() + part.drop(1).lowercaseAscii()
            else -> part.lowercaseAscii()
        }
    }.joinToString("-")
}

/**
 * ASCII-only case folding, and that is the point rather than an oversight.
 *
 * `lowercase()` with a locale is how `I` becomes `ı` on a Turkish phone — `docs/localization.md` §3
 * flags exactly this — and a tag that folded differently depending on who was holding the phone
 * would stop matching the tag stored beside it. A language tag is ASCII by definition, so this
 * cannot lose anything.
 */
private fun String.lowercaseAscii(): String = map { if (it in 'A'..'Z') it + CASE_GAP else it }
    .joinToString("")

private fun String.uppercaseAscii(): String = map { if (it in 'a'..'z') it - CASE_GAP else it }
    .joinToString("")

private fun Char.isLatinLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

private const val CASE_GAP = 'a' - 'A'

private const val REGION_LENGTH = 2

private const val SCRIPT_LENGTH = 4

/** The shape of a language tag: letters, then any number of alphanumeric subtags. */
private val TAG = Regex("[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*")
