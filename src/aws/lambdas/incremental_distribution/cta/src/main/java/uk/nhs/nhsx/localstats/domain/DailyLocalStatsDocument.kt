package uk.nhs.nhsx.localstats.domain

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.localstats.domain.AreaCode.Companion.ENGLAND
import uk.nhs.nhsx.localstats.domain.AreaCode.Companion.WALES
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDate
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateChange
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateChangePercentage
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateDirection
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateRollingSum
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesBySpecimenDateRollingRate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class DailyLocalStatsDocument(
    val lastFetch: Instant,
    val metadata: Metadata,
    val england: CountryMetric,
    val wales: CountryMetric,
    val lowerTierLocalAuthorities: Map<AreaCode, DailyLocalAuthorityMetrics?>
)

data class Metadata(
    val england: CountryMetadata,
    val wales: CountryMetadata,
    val lowerTierLocalAuthorities: AuthoritiesMetadata
)

data class CountryMetadata(val newCasesBySpecimenDateRollingRate: MetricsInfo)

data class AuthoritiesMetadata(
    val newCasesByPublishDate: MetricsInfo,
    val newCasesByPublishDateChange: MetricsInfo,
    val newCasesByPublishDateDirection: MetricsInfo,
    val newCasesByPublishDateRollingSum: MetricsInfo,
    val newCasesBySpecimenDateRollingRate: MetricsInfo,
    val newCasesByPublishDateChangePercentage: MetricsInfo,
)

data class MetricsInfo(val lastUpdate: LocalDate)

data class CountryMetric(
    val newCasesBySpecimenDateRollingRate: BigDecimal?
)

data class DailyLocalAuthorityMetrics(
    val name: AreaName,
    val newCasesByPublishDate: BigDecimal?,
    val newCasesByPublishDateChange: BigDecimal?,
    val newCasesByPublishDateDirection: Direction?,
    val newCasesByPublishDateRollingSum: BigDecimal?,
    val newCasesBySpecimenDateRollingRate: BigDecimal?,
    val newCasesByPublishDateChangePercentage: BigDecimal?
)

class DailyLocalStatsDocumentBuilder(
    private val clock: Clock
) {
    fun build(
        authorityMetrics: MetricTable,
        nationMetrics: MetricTable,
    ) = DailyLocalStatsDocument(
        lastFetch = clock(),
        metadata = Metadata(
            england = CountryMetadata(
                newCasesBySpecimenDateRollingRate = MetricsInfo(
                    nationMetrics.lastUpdatedOnOrThrow(newCasesBySpecimenDateRollingRate)
                )
            ),
            wales = CountryMetadata(
                newCasesBySpecimenDateRollingRate = MetricsInfo(
                    nationMetrics.lastUpdatedOnOrThrow(newCasesBySpecimenDateRollingRate)
                )
            ),
            lowerTierLocalAuthorities = AuthoritiesMetadata(
                newCasesByPublishDate = MetricsInfo(
                    authorityMetrics.lastUpdatedOnOrThrow(newCasesByPublishDate)
                ),
                newCasesByPublishDateChange = MetricsInfo(
                    authorityMetrics.lastUpdatedOnOrThrow(newCasesByPublishDateChange)
                ),
                newCasesByPublishDateDirection = MetricsInfo(
                    authorityMetrics.lastUpdatedOnOrThrow(newCasesByPublishDateDirection)
                ),
                newCasesByPublishDateRollingSum = MetricsInfo(
                    authorityMetrics.lastUpdatedOnOrThrow(newCasesByPublishDateRollingSum)
                ),
                newCasesBySpecimenDateRollingRate = MetricsInfo(
                    authorityMetrics.lastUpdatedOnOrThrow(newCasesBySpecimenDateRollingRate)
                ),
                newCasesByPublishDateChangePercentage = MetricsInfo(
                    authorityMetrics.lastUpdatedOnOrThrow(newCasesByPublishDateChangePercentage)
                ),
            )
        ),
        england = CountryMetric(
            newCasesBySpecimenDateRollingRate = nationMetrics
                .areaCode(ENGLAND)
                .latest(newCasesBySpecimenDateRollingRate)
                .toMetricValue()
                .toBigDecimal()
        ),
        wales = CountryMetric(
            newCasesBySpecimenDateRollingRate = nationMetrics
                .areaCode(WALES)
                .latest(newCasesBySpecimenDateRollingRate)
                .toMetricValue()
                .toBigDecimal()
        ),
        lowerTierLocalAuthorities = authorityMetrics.areaCodes().associate { area ->
            area.areaCode() to DailyLocalAuthorityMetrics(
                name = area.areName(),
                newCasesByPublishDate = area
                    .latest(newCasesByPublishDate)
                    .toMetricValue()
                    .toBigDecimal(),
                newCasesByPublishDateChange = area
                    .latest(newCasesByPublishDateChange)
                    .toMetricValue()
                    .toBigDecimal(),
                newCasesByPublishDateDirection = area
                    .latest(newCasesByPublishDateDirection)
                    .toMetricValue()
                    .toDirection(),
                newCasesByPublishDateRollingSum = area
                    .latest(newCasesByPublishDateRollingSum)
                    .toMetricValue()
                    .toBigDecimal(),
                newCasesBySpecimenDateRollingRate = area
                    .latest(newCasesBySpecimenDateRollingRate)
                    .toMetricValue()
                    .toBigDecimal(),
                newCasesByPublishDateChangePercentage = area
                    .latest(newCasesByPublishDateChangePercentage)
                    .toMetricValue()
                    .toBigDecimal()
            )
        }
    )
}

