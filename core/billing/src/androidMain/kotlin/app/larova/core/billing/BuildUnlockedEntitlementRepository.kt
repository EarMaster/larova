package app.larova.core.billing

import app.larova.core.domain.model.Entitlement
import app.larova.core.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The answer for a build that has no paid tier: everything is available, and nothing was bought.
 *
 * This is what the APK attached to the GitHub Release uses. It is not a hole left open by accident.
 * Larova is AGPL-3.0, so anybody may delete the check, rebuild and publish the result — legally,
 * and in about five minutes. A sideload build that pretended to a lock it cannot keep would be
 * dishonest about that and would annoy exactly the people most likely to contribute; one that says
 * "built from source, no store behind it" is true.
 *
 * [Entitlement.BUILD] rather than [Entitlement.PLAY] on purpose. The settings screen can then say
 * something accurate, and a bug report says which kind of build it came from without asking.
 */
class BuildUnlockedEntitlementRepository : EntitlementRepository {

    override fun observe(): Flow<Entitlement> = flowOf(Entitlement.BUILD)

    /** There is no store to ask. Not an error, and not worth reporting. */
    override suspend fun refresh() = Unit

    /** Nothing is for sale in this build, so there is no price to name. */
    override suspend fun formattedPrice(): String? = null
}
