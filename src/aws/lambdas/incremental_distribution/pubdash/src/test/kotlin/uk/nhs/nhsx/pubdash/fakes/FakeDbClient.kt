package uk.nhs.nhsx.pubdash.fakes

import com.amazonaws.services.athena.model.GetQueryResultsResult
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult
import uk.nhs.nhsx.pubdash.persistence.AsyncDbClient

class FakeDbClient(
    resultQueryIds: List<QueryId>,
    private val queryResults: Map<QueryId, List<QueryResult<GetQueryResultsResult>>> = emptyMap()
) : AsyncDbClient {

    private val submittedQueries = mutableListOf<String>()
    private val queryIds = resultQueryIds.toMutableList()
    private val queryIdResultIndex: MutableMap<QueryId, Int> = queryResults
        .map { (key, _) -> key to 0 }.toMap().toMutableMap()

    override fun submitQuery(sqlQuery: String): QueryId {
        submittedQueries.add(sqlQuery)
        return queryIds.removeFirst()
    }

    override fun queryResults(queryId: QueryId): QueryResult<GetQueryResultsResult> {
        val index = queryIdResultIndex[queryId]!!
        queryIdResultIndex[queryId] = index + 1
        return queryResults[queryId]!![index]
    }

    fun submittedSqlQueries() = submittedQueries.toList()
    fun lastSubmittedSqlQuery() = submittedQueries.last()
}
