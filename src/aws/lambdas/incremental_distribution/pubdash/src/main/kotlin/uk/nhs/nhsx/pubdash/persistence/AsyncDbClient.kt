package uk.nhs.nhsx.pubdash.persistence

import com.amazonaws.services.athena.model.GetQueryResultsResult
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult

interface AsyncDbClient {
    fun submitQuery(sqlQuery: String): QueryId
    fun queryResults(queryId: QueryId): QueryResult<GetQueryResultsResult>
}
