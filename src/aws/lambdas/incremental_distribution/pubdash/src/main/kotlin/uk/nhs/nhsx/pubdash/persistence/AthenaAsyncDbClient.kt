package uk.nhs.nhsx.pubdash.persistence

import com.amazonaws.services.athena.AmazonAthena
import com.amazonaws.services.athena.model.GetQueryExecutionRequest
import com.amazonaws.services.athena.model.GetQueryExecutionResult
import com.amazonaws.services.athena.model.GetQueryResultsRequest
import com.amazonaws.services.athena.model.GetQueryResultsResult
import com.amazonaws.services.athena.model.QueryExecutionState
import com.amazonaws.services.athena.model.StartQueryExecutionRequest
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult

class AthenaAsyncDbClient(private val athena: AmazonAthena, private val workgroup: String) : AsyncDbClient {

    override fun submitQuery(sqlQuery: String): QueryId =
        QueryId(athena.startQueryExecution(startQueryExecutionRequest(sqlQuery)).queryExecutionId)

    private fun startQueryExecutionRequest(sqlQuery: String) =
        StartQueryExecutionRequest()
            .withWorkGroup(workgroup)
            .withQueryString(sqlQuery)

    override fun queryResults(queryId: QueryId): QueryResult<GetQueryResultsResult> = athena.queryResult(queryId)

    private fun AmazonAthena.queryResult(queryId: QueryId): QueryResult<GetQueryResultsResult> {
        val queryExecutionResult = getQueryExecutionResult(queryId)
        return when (queryExecutionResult.queryExecution.status.state) {
            QueryExecutionState.SUCCEEDED.toString() -> QueryResult.Finished(getQueryResults(queryId))
            QueryExecutionState.FAILED.toString() -> QueryResult.Error(errorMessageFrom(queryExecutionResult))
            QueryExecutionState.CANCELLED.toString() -> QueryResult.Error("The Amazon Athena query was cancelled.")
            else -> QueryResult.Waiting()
        }
    }

    private fun AmazonAthena.getQueryResults(queryId: QueryId) =
        getQueryResults(GetQueryResultsRequest().withQueryExecutionId(queryId.id))

    private fun AmazonAthena.getQueryExecutionResult(queryId: QueryId): GetQueryExecutionResult {
        val executionId = GetQueryExecutionRequest().withQueryExecutionId(queryId.id)
        return getQueryExecution(executionId)
    }

    private fun errorMessageFrom(queryExecutionResult: GetQueryExecutionResult) =
        "The Amazon Athena query failed to run with error message: " +
            queryExecutionResult.queryExecution.status.stateChangeReason
}
