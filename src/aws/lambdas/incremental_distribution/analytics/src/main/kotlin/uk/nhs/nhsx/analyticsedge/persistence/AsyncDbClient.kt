package uk.nhs.nhsx.analyticsedge.persistence

import uk.nhs.nhsx.analyticsedge.QueryId
import uk.nhs.nhsx.analyticsedge.QueryResult

interface AsyncDbClient {
    fun submitQuery(sqlQuery: String): QueryId
    fun queryResults(queryId: QueryId): QueryResult<Unit>
}
