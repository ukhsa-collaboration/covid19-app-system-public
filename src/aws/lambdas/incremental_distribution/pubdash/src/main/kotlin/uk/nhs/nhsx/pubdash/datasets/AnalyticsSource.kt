package uk.nhs.nhsx.pubdash.datasets

import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult

interface AnalyticsSource {
    fun checkQueryState(queryId: QueryId): QueryResult<Unit>

    fun startAgnosticDatasetQueryAsync(): QueryId
    fun startCountryDatasetQueryAsync(): QueryId
    fun startLocalAuthorityDatasetQueryAsync(): QueryId
}
