package uk.nhs.nhsx.pubdash

import com.amazonaws.services.athena.AmazonAthena
import com.amazonaws.services.athena.model.GetQueryExecutionRequest
import com.amazonaws.services.athena.model.GetQueryResultsRequest
import com.amazonaws.services.athena.model.GetQueryResultsResult
import com.amazonaws.services.athena.model.QueryExecutionState
import com.amazonaws.services.athena.model.StartQueryExecutionRequest
import java.time.Duration

class DbClient(
    private val athena: AmazonAthena,
    private val maxPollAttempts: Int = 60,
    private val pollSleepDuration: Duration = Duration.ofSeconds(1)
) {

    fun query(sql: String) = athena.query(sql)

    private fun AmazonAthena.query(query: String) =
        pollQueryResult(startQueryExecution(StartQueryExecutionRequest().withQueryString(query)).queryExecutionId)

    private fun AmazonAthena.pollQueryResult(queryExecutionId: String): GetQueryResultsResult {
        var attempts = 0
        while (attempts++ < maxPollAttempts) {
            val executionId = GetQueryExecutionRequest().withQueryExecutionId(queryExecutionId)
            val getQueryExecutionResponse = getQueryExecution(executionId)
            when (getQueryExecutionResponse.queryExecution.status.state) {
                QueryExecutionState.SUCCEEDED.toString() -> return getQueryResults(
                    GetQueryResultsRequest().withQueryExecutionId(
                        queryExecutionId
                    )
                )
                QueryExecutionState.FAILED.toString() -> throw RuntimeException("The Amazon Athena query failed to run with error message: " + getQueryExecutionResponse.queryExecution.status.stateChangeReason)
                QueryExecutionState.CANCELLED.toString() -> throw RuntimeException("The Amazon Athena query was cancelled.")
                else -> Thread.sleep(pollSleepDuration.toMillis())
            }
        }

        throw RuntimeException("Query exceeded $attempts seconds")
    }
}
