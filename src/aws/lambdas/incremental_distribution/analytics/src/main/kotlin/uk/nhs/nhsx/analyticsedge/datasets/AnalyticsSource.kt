package uk.nhs.nhsx.analyticsedge.datasets

import uk.nhs.nhsx.analyticsedge.QueryId
import uk.nhs.nhsx.analyticsedge.QueryResult

interface AnalyticsSource {

    fun startAdoptionDatasetQueryAsync(): QueryId
    fun startAggregateDatasetQueryAsync(): QueryId
    fun startEnpicDatasetQueryAsync(): QueryId
    fun startIsolationDatasetQueryAsync(): QueryId
    fun startPosterDatasetQueryAsync(): QueryId

    fun checkQueryState(queryId: QueryId): QueryResult<Unit>
}
