package uk.nhs.nhsx.analyticssubmission.policy

import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Clock
import java.time.LocalDate
import java.time.ZoneId

abstract class GovernmentPolicy(private val value: LocalDate) {
    fun isInEffect(clock: Clock) = LocalDate.ofInstant(clock(), ZoneId.of("Europe/London")) >= value

    abstract fun scrub(payload: ClientAnalyticsSubmissionPayload): ClientAnalyticsSubmissionPayload
}
