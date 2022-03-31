package uk.nhs.nhsx.analyticssubmission.model

import uk.nhs.nhsx.core.Clock
import java.time.Duration
import java.time.Instant

data class AnalyticsWindow(
    val startDate: Instant,
    val endDate: Instant
) {

    fun isDateRangeInvalid(clock: Clock): Boolean {
        val outOfScope = clock().plus(Duration.ofDays(365))
        return startDate.isAfter(outOfScope) || endDate.isAfter(outOfScope)
    }
}
