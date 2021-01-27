package smoke.actors

import com.amazonaws.services.athena.AmazonAthena
import com.amazonaws.services.athena.AmazonAthenaClient
import com.amazonaws.services.athena.model.GetQueryExecutionRequest
import com.amazonaws.services.athena.model.GetQueryResultsRequest
import com.amazonaws.services.athena.model.GetQueryResultsResult
import com.amazonaws.services.athena.model.QueryExecutionState
import com.amazonaws.services.athena.model.StartQueryExecutionRequest
import smoke.env.EnvConfig

class Analytics(private val envConfig: EnvConfig) {

    private val athenaClient = AmazonAthenaClient.builder().build()

    fun getRecordedAnalyticsFor(deviceModel: MobileDeviceModel): List<Pair<String, String>> {
        val results = athenaClient.queryAndWait("""
                                        SELECT * FROM "${envConfig.workspaceName}_analytics_db"."${envConfig.workspaceName}_analytics_mobile"
                                        where "devicemodel" = '${deviceModel.value}'
                                        limit 1;
                                    """)
        if (results.resultSet.rows.size < 2) return emptyList()

        val rowNames = results.resultSet.rows[0].data.map { it.varCharValue }
        val values = results.resultSet.rows[1].data.map { it.varCharValue }
        return rowNames.zip(values)
    }
}

private fun AmazonAthena.queryAndWait(query: String) =
    waitForQueryToComplete(
        startQueryExecution(StartQueryExecutionRequest().withQueryString(query)).queryExecutionId
    )

private fun AmazonAthena.waitForQueryToComplete(queryExecutionId: String): GetQueryResultsResult {
    var attempts = 0
    while (attempts++ < 60) {
        val getQueryExecutionResponse = getQueryExecution(GetQueryExecutionRequest().withQueryExecutionId(queryExecutionId))
        when (getQueryExecutionResponse.queryExecution.status.state.toString()) {
            QueryExecutionState.FAILED.toString() -> throw RuntimeException("The Amazon Athena query failed to run with error message: " + getQueryExecutionResponse
                .queryExecution.status.stateChangeReason)
            QueryExecutionState.CANCELLED.toString() -> throw RuntimeException("The Amazon Athena query was cancelled.")
            QueryExecutionState.SUCCEEDED.toString() -> return getQueryResults(GetQueryResultsRequest().withQueryExecutionId(queryExecutionId))
            else -> Thread.sleep(1000)
        }
    }

    throw RuntimeException("Query exceeded $attempts seconds")
}
