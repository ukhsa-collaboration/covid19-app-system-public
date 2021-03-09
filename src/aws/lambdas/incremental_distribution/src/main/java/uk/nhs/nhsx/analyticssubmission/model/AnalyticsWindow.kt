package uk.nhs.nhsx.analyticssubmission.model

import java.time.Instant

data class AnalyticsWindow(@JvmField val startDate: Instant, @JvmField val endDate: Instant)
