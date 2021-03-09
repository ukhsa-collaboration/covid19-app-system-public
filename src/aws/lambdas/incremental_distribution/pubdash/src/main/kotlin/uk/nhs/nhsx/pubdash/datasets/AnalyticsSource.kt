package uk.nhs.nhsx.pubdash.datasets

import uk.nhs.nhsx.pubdash.CsvS3Object
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult

interface AnalyticsSource {
    fun agnosticDataset(queryId: QueryId): QueryResult<CsvS3Object>
    fun countryDataset(queryId: QueryId): QueryResult<CsvS3Object>
    fun localAuthorityDataset(queryId: QueryId): QueryResult<CsvS3Object>

    fun startAgnosticDatasetQueryAsync(): QueryId
    fun startCountryDatasetQueryAsync(): QueryId
    fun startLocalAuthorityDatasetQueryAsync(): QueryId
}
