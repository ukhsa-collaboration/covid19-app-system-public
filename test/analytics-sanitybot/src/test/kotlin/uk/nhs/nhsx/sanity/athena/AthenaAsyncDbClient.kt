package uk.nhs.nhsx.sanity.athena

import com.amazonaws.services.athena.AmazonAthena
import com.amazonaws.services.athena.model.GetQueryExecutionRequest
import com.amazonaws.services.athena.model.GetQueryExecutionResult
import com.amazonaws.services.athena.model.QueryExecutionContext
import com.amazonaws.services.athena.model.QueryExecutionState
import com.amazonaws.services.athena.model.ResultConfiguration
import com.amazonaws.services.athena.model.StartQueryExecutionRequest

class AthenaAsyncDbClient(
    private val athena: AmazonAthena,
    private val athenaDb: String,
    private val outputBucket: String) : AsyncDbClient {

    override fun submitQuery(sqlQuery: String): QueryId =
        QueryId(athena.startQueryExecution(startQueryExecutionRequest(sqlQuery)).queryExecutionId)

    private fun startQueryExecutionRequest(sqlQuery: String) =
        StartQueryExecutionRequest()
            .withQueryExecutionContext(
                QueryExecutionContext().withDatabase(athenaDb)
            )
            .withResultConfiguration(
                ResultConfiguration().withOutputLocation(outputBucket)
            )
            .withQueryString(sqlQuery)

    override fun queryResults(queryId: QueryId): QueryResult<Unit> = queryResult(queryId)

    private fun queryResult(queryId: QueryId): QueryResult<Unit> {
        val queryExecutionResult = queryExecutionResult(queryId)
        return when (queryExecutionResult.queryExecution.status.state) {
            QueryExecutionState.SUCCEEDED.toString() -> QueryResult.Finished(Unit)
            QueryExecutionState.FAILED.toString() -> QueryResult.Error(errorMessageFrom(queryExecutionResult))
            QueryExecutionState.CANCELLED.toString() -> QueryResult.Error("The Amazon Athena query was cancelled.")
            else -> QueryResult.Waiting()
        }
    }

    private fun queryExecutionResult(queryId: QueryId): GetQueryExecutionResult {
        val executionId = GetQueryExecutionRequest().withQueryExecutionId(queryId.id)
        return athena.getQueryExecution(executionId)
    }

    private fun errorMessageFrom(queryExecutionResult: GetQueryExecutionResult) =
        "The Amazon Athena query failed to run with error message: " +
            queryExecutionResult.queryExecution.status.stateChangeReason
}
