package uk.nhs.nhsx.localstats

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.localstats.data.CoronavirusApi
import uk.nhs.nhsx.localstats.data.actions.DownloadMetrics
import uk.nhs.nhsx.localstats.data.actions.MetricAvailability
import uk.nhs.nhsx.localstats.domain.DailyLocalStatsDocument
import uk.nhs.nhsx.localstats.domain.DailyLocalStatsDocumentBuilder
import uk.nhs.nhsx.localstats.domain.InMemoryMetricTable
import uk.nhs.nhsx.localstats.domain.LowerTierLocalAuthority
import uk.nhs.nhsx.localstats.domain.Nation
import uk.nhs.nhsx.localstats.domain.ReleaseDate

class DailyLocalStats(
    private val coronavirusApi: CoronavirusApi,
    private val clock: Clock
) {
    fun generateDocument(releaseDate: ReleaseDate): DailyLocalStatsDocument {
        val nationMetrics = coronavirusApi(DownloadMetrics(Nation, releaseDate))
        val authorityMetrics = coronavirusApi(DownloadMetrics(LowerTierLocalAuthority, releaseDate))
        val nationMetadata = coronavirusApi(MetricAvailability(Nation))
        val authorityMetadata = coronavirusApi(MetricAvailability(LowerTierLocalAuthority))

        return DailyLocalStatsDocumentBuilder(clock)
            .build(
                authorityMetrics = InMemoryMetricTable(
                    authorityMetrics.metrics,
                    authorityMetadata
                ),
                nationMetrics = InMemoryMetricTable(
                    nationMetrics.metrics,
                    nationMetadata
                )
            )
    }
}
