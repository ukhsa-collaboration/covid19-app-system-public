package uk.nhs.nhsx.localstats.data

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.map
import uk.nhs.nhsx.localstats.data.CoronavirusApiResponses.LTLA_HARINGEY_DAILY_FULL
import uk.nhs.nhsx.localstats.data.actions.CoronavirusDataCSVParser
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric
import uk.nhs.nhsx.localstats.domain.CoronavirusMetric.*
import uk.nhs.nhsx.localstats.domain.LowerTierLocalAuthority
import uk.nhs.nhsx.localstats.domain.Metric

class CoronavirusDataCSVParserTest {

    @Test
    fun `can parse full LTLA response`() {
        val metrics = CoronavirusDataCSVParser(LowerTierLocalAuthority.metrics)
            .parse(LTLA_HARINGEY_DAILY_FULL.read())

        expectThat(metrics)
            .hasSize(2870)
            .map(Metric::value)
            .map(Pair<CoronavirusMetric, Any?>::first)
            .contains(
                newCasesByPublishDate,
                newCasesByPublishDateChange,
                newCasesByPublishDateChangePercentage,
                newCasesByPublishDateDirection,
                newCasesByPublishDateRollingSum
            )
    }
}
