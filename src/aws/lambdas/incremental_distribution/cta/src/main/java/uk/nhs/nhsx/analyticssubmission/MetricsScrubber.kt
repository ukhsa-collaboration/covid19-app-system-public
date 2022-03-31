@file:Suppress("SpellCheckingInspection")

package uk.nhs.nhsx.analyticssubmission

import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.analyticssubmission.policy.PolicyConfig
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events

class MetricsScrubber(
    private val events: Events,
    private val clock: Clock,
    private val config: PolicyConfig
) {

    fun scrub(payload: ClientAnalyticsSubmissionPayload): ClientAnalyticsSubmissionPayload {
        val (postalDistrict, localAuthority) = PostDistrictLaReplacer(
            postDistrict = payload.metadata.postalDistrict,
            localAuthority = payload.metadata.localAuthority,
            events = events
        )

        val model = payload.copy(
            metadata = payload.metadata.copy(
                postalDistrict = postalDistrict,
                localAuthority = localAuthority
            )
        )

        return config.policies
            .filter { it.isInEffect(clock) }
            .fold(model) { analytics, policy -> policy.scrub(analytics) }
    }
}
