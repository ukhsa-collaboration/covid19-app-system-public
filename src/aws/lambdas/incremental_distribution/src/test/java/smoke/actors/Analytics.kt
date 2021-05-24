package smoke.actors

import org.http4k.aws.AwsSdkClient
import org.http4k.core.HttpHandler
import smoke.env.EnvConfig
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.QueryExecutionState.CANCELLED
import software.amazon.awssdk.services.athena.model.QueryExecutionState.FAILED
import software.amazon.awssdk.services.athena.model.QueryExecutionState.SUCCEEDED
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import uk.nhs.nhsx.core.Clock
import java.lang.Thread.sleep
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Analytics(private val envConfig: EnvConfig, http: HttpHandler, private val clock: Clock) {

    private val athenaClient = AthenaClient.builder().httpClient(AwsSdkClient(http)).build()

    fun getRecordedAnalyticsFor(deviceModel: MobileDeviceModel): List<Pair<String, String>> {
        val results = athenaClient.queryAndWait(
            """
                SELECT * FROM "${envConfig.workspace_name}-analytics"."${envConfig.workspace_name}_analytics" 
                where devicemodel = '${deviceModel.value}'
                limit 1;
            """
        )

        if (results.resultSet().rows().size < 2) return emptyList()
        val rowNames = results.resultSet().rows()[0].data().map { it.varCharValue() }
        val values = results.resultSet().rows()[1].data().map { it.varCharValue() }
        return rowNames.zip(values).also { println(it) }
    }
}

private fun AthenaClient.queryAndWait(query: String) =
    waitForQueryToComplete(
        startQueryExecution(StartQueryExecutionRequest.builder().queryString(query).build()).queryExecutionId()
    )

private fun AthenaClient.waitForQueryToComplete(queryExecutionId: String): GetQueryResultsResponse {
    var attempts = 0
    while (attempts++ < 60) {
        val getQueryExecutionResponse =
            getQueryExecution(GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build())
        when (getQueryExecutionResponse.queryExecution().status().state()) {
            FAILED -> throw RuntimeException(
                "The Amazon Athena query failed to run with error message: " + getQueryExecutionResponse
                    .queryExecution().status().stateChangeReason()
            )
            CANCELLED -> throw RuntimeException("The Amazon Athena query was cancelled.")
            SUCCEEDED ->
                return getQueryResults(GetQueryResultsRequest.builder().queryExecutionId(queryExecutionId).build())
            else -> sleep(5000)
        }
    }

    throw RuntimeException("Query exceeded $attempts seconds")
}

