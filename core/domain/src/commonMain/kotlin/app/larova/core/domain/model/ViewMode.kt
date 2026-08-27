package app.larova.core.domain.model

/**
 * Who is holding the phone, as far as the interface is concerned.
 *
 * The single most consequential decision in the product (docs/concept.md §4.2). Caregiver view is
 * the default and the normal state: read, tick, call, open. Parent view is a temporary unlock, not
 * a login — there is no account, and nobody has to understand a permission model.
 *
 * [CAREGIVER] is deliberately the value a fresh session starts in. Nothing persists an unlock, so
 * a phone that was left in parent view and picked up an hour later is back to caregiver view.
 */
enum class ViewMode {
    CAREGIVER,
    PARENT,
    ;

    val isParent: Boolean get() = this == PARENT
}
