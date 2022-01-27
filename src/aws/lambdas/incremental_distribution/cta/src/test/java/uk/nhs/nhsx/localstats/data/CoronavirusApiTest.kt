package uk.nhs.nhsx.localstats.data

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.localstats.data.actions.DownloadMetrics
import uk.nhs.nhsx.localstats.data.actions.LatestReleaseTimestamp
import uk.nhs.nhsx.localstats.data.actions.MetricAvailability
import uk.nhs.nhsx.localstats.domain.AreaCode
import uk.nhs.nhsx.localstats.domain.AreaName
import uk.nhs.nhsx.localstats.domain.AreaTypeCode
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDate
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateChange
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateChangePercentage
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateDirection
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesByPublishDateRollingSum
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.newCasesBySpecimenDateRollingRate
import uk.nhs.nhsx.localstats.domain.LowerTierLocalAuthority
import uk.nhs.nhsx.localstats.domain.Metric
import uk.nhs.nhsx.localstats.domain.MetricName
import uk.nhs.nhsx.localstats.domain.MetricResponse
import uk.nhs.nhsx.localstats.domain.ReleaseDate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class CoronavirusApiTest {

    @Nested
    inner class DownloadingMetrics {
        @Test
        fun `throws exception`() {
            val backend = FakeCoronavirusApiBackend().apply { misbehave() }
            val api = CoronavirusApi.Http(backend, backoff = Duration.ofMillis(10))

            expectThrows<IllegalStateException> {
                api(DownloadMetrics(LowerTierLocalAuthority))
            }
        }

        @Test
        fun `downloads all metrics`() {
            val coronavirusApi = CoronavirusApi.Http(FakeCoronavirusApiBackend())
            val response = coronavirusApi(DownloadMetrics(LowerTierLocalAuthority))

            expectThat(response) {
                get(MetricResponse::areaType).isEqualTo(LowerTierLocalAuthority)
                get(MetricResponse::metrics).containsExactlyInAnyOrder(
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-16"),
                        value = newCasesByPublishDateChangePercentage to "36.7"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-16"),
                        value = newCasesByPublishDateChange to "207"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-16"),
                        value = newCasesByPublishDateDirection to "UP"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-16"),
                        value = newCasesByPublishDate to "105"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-15"),
                        value = newCasesByPublishDateChangePercentage to "25.6"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-15"),
                        value = newCasesByPublishDateChange to "150"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-15"),
                        value = newCasesByPublishDateDirection to "UP"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-15"),
                        value = newCasesByPublishDate to "100"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-16"),
                        value = newCasesByPublishDateRollingSum to "771"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-15"),
                        value = newCasesByPublishDateRollingSum to "735"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-11"),
                        value = newCasesBySpecimenDateRollingRate to "255.3"
                    ),
                    Metric(
                        areaType = AreaTypeCode("ltla"),
                        areaCode = AreaCode("E09000014"),
                        areaName = AreaName("Haringey"),
                        date = LocalDate.parse("2021-11-10"),
                        value = newCasesBySpecimenDateRollingRate to "241.8"
                    )
                )
            }
        }
    }

    @Nested
    inner class LastRelease {
        @Test
        fun `throws exception`() {
            val backend = FakeCoronavirusWebsiteBackend().apply { misbehave() }
            val api = CoronavirusWebsite.Http(backend, backoff = Duration.ofMillis(10))

            expectThrows<IllegalStateException> {
                api(LatestReleaseTimestamp)
            }
        }

        @Test
        fun `download last updated date`() {
            val backend = FakeCoronavirusWebsiteBackend(Instant.EPOCH)
            val lastUpdated = CoronavirusWebsite.Http(backend)(LatestReleaseTimestamp)
            expectThat(lastUpdated).isEqualTo(ReleaseDate.of(Instant.EPOCH))
        }
    }

    @Nested
    inner class MetricAvailability {
        @Test
        fun `throws exception`() {
            val backend = FakeCoronavirusApiBackend().apply { misbehave() }
            val api = CoronavirusApi.Http(backend, backoff = Duration.ofMillis(10))

            expectThrows<IllegalStateException> {
                api(MetricAvailability(LowerTierLocalAuthority))
            }
        }

        @Test
        fun `downloads available metrics`() {
            val coronavirusApi = CoronavirusApi.Http(FakeCoronavirusApiBackend())
            val lookup = coronavirusApi(MetricAvailability(LowerTierLocalAuthority))
            val lastUpdated = lookup[MetricName.of(newCasesByPublishDateChange.name)]
            expectThat(lastUpdated).isEqualTo(LocalDate.of(2021, 11, 16))
        }
    }
}

