package uk.nhs.nhsx.pubdash.datasets

import uk.nhs.nhsx.pubdash.CsvS3Object
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult
import kotlin.random.Random

class FakeSource(
    private val source: AnalyticsSource,
    private val random: Random = Random.Default
) : AnalyticsSource {

    override fun agnosticDataset(queryId: QueryId): QueryResult<CsvS3Object> =
        when (val dataset = source.agnosticDataset(queryId)) {
            is QueryResult.Error -> dataset
            is QueryResult.Finished -> QueryResult.Finished(
                AgnosticDataset((dataset.results as AgnosticDataset).data.map { it.fake() })
            )
            is QueryResult.Waiting -> dataset
        }

    override fun countryDataset(queryId: QueryId): QueryResult<CsvS3Object> =
        when (val dataset = source.countryDataset(queryId)) {
            is QueryResult.Error -> dataset
            is QueryResult.Finished -> QueryResult.Finished(
                CountryDataset((dataset.results as CountryDataset).data.map { it.fake() })
            )
            is QueryResult.Waiting -> dataset
        }

    override fun localAuthorityDataset(queryId: QueryId): QueryResult<CsvS3Object> =
        when (val dataset = source.localAuthorityDataset(queryId)) {
            is QueryResult.Error -> dataset
            is QueryResult.Finished -> QueryResult.Finished(
                LocalAuthorityDataset((dataset.results as LocalAuthorityDataset).data.map { it.fake() })
            )
            is QueryResult.Waiting -> dataset
        }

    private fun AgnosticDatasetRow.fake() =
        AgnosticDatasetRow(
            weekEnding = weekEnding,
            downloads = random.nextLong(100000, 1000000),
            posters = random.nextLong(100, 100000),
            riskyVenues = random.nextLong(1, 100)
        )

    private fun CountryDatasetRow.fake() =
        CountryDatasetRow(
            weekEnding = weekEnding,
            countryEnglish = countryEnglish,
            countryWelsh = countryWelsh,
            contactTracingAlerts = random.nextLong(100, 100000),
            checkIns = random.nextLong(100, 100000),
            positiveTestResults = random.nextLong(100, 100000),
            negativeTestResults = random.nextLong(100, 100000),
            symptomsReported = random.nextLong(100, 100000)
        )

    private fun LocalAuthorityDatasetRow.fake() =
        LocalAuthorityDatasetRow(
            weekEnding = weekEnding,
            localAuthority = localAuthority,
            contactTracingAlerts = random.nextLong(100, 100000),
            checkIns = random.nextLong(100, 100000),
            positiveTestResults = random.nextLong(100, 100000),
            negativeTestResults = random.nextLong(100, 100000),
            symptomsReported = random.nextLong(100, 100000)
        )

    override fun startAgnosticDatasetQueryAsync(): QueryId = source.startAgnosticDatasetQueryAsync()

    override fun startCountryDatasetQueryAsync(): QueryId = source.startCountryDatasetQueryAsync()

    override fun startLocalAuthorityDatasetQueryAsync(): QueryId = source.startLocalAuthorityDatasetQueryAsync()
}
