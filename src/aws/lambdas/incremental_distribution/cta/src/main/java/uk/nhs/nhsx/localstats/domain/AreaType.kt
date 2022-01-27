package uk.nhs.nhsx.localstats.domain

import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDate
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateChange
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateChangePercentage
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateDirection
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateRollingSum
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesBySpecimenDateRollingRate

sealed class AreaType(
    val metrics: Set<CoronavirusMetric>
)

object LowerTierLocalAuthority : AreaType(
    setOf(
        newCasesByPublishDate,
        newCasesByPublishDateChange,
        newCasesByPublishDateDirection,
        newCasesByPublishDateRollingSum,
        newCasesBySpecimenDateRollingRate,
        newCasesByPublishDateChangePercentage
    )
)

object Nation : AreaType(
    setOf(
        newCasesBySpecimenDateRollingRate
    )
)
