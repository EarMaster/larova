package app.larova.core.domain.model

/**
 * Whether this installation may author the tile types that are sold, and what says so.
 *
 * One type rather than a boolean beside a source, because the two can disagree and a build that
 * thinks it is unlocked by nothing is exactly the bug this would be blamed for. The source is
 * carried because the settings screen has to say something true about *why* it is unlocked, and
 * because "this build has no paid tier" and "a purchase was checked on this device" are different
 * enough facts to be worth telling apart in a bug report.
 *
 * There is deliberately no `EXPIRED` or `PENDING`. The unlock is bought once and never lapses, so a
 * state that means "used to be paid" would be a state nothing is allowed to produce.
 */
enum class Entitlement(val unlocked: Boolean) {

    /** Nothing has vouched for this installation. The tile types that are sold cannot be made. */
    NONE(unlocked = false),

    /** A Play purchase, re-checked against the licensing key on this device rather than trusted. */
    PLAY(unlocked = true),

    /** A signed unlock key entered by hand — the path for builds Play cannot sell to. */
    KEY(unlocked = true),

    /**
     * This build has no paid tier at all, which is the honest answer for anything built from
     * source. The licence lets anyone remove the check and rebuild in five minutes, so the source
     * build says so up front instead of pretending to a lock it cannot keep.
     */
    BUILD(unlocked = true),
}

/**
 * A purchase exactly as the store handed it over: the payload that was signed, and the signature.
 *
 * Kept as the two original strings rather than as a parsed object or a boolean, because the whole
 * point is to be able to check it again later, offline, without asking anything. A stored `true`
 * would be a claim; this is evidence.
 */
data class Receipt(
    val payload: String,
    val signature: String,
)
