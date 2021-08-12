package smoke.actors

import org.http4k.core.Status.Companion.OK
import org.joda.time.DateTime
import smoke.clients.AwsLambda
import smoke.env.EnvConfig
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.virology.tokengen.CtaProcessorRequest
import java.time.Instant

class BackgroundActivities(private val envConfig: EnvConfig) {

    fun invokesBatchProcessing() {

        AwsLambda.invokeFunction(envConfig.diagnosis_keys_processing_function)
            .requireStatusCode(OK)
            .requireBodyContains("KeysDistributed")

        AwsLambda.invokeFunction(envConfig.federation_keys_processing_download_function)
            .requireStatusCode(OK)
            .requireBodyContains("InteropConnectorDownloadStats")

        AwsLambda.invokeFunction(envConfig.federation_keys_processing_upload_function)
            .requireStatusCode(OK)
            .requireBodyContains("InteropConnectorUploadStats")
    }

    fun invokeAnalyticsLogs(scheduledEventTime: Instant, functionName: String): InvokeResponse {
        return AwsLambda.invokeFunction(functionName, """{ "time": "${DateTime(scheduledEventTime.toEpochMilli())}" }""")
            .requireStatusCode(OK)
    }

    fun invokeVirologyTokenProcessor(request: CtaProcessorRequest) = AwsLambda.invokeFunction(
        functionName = envConfig.virology_tokens_processing_function,
        payload = Json.toJson(request)
    ).requireStatusCode(OK)

    fun invokeScheduledVirologyTokenProcessor() = AwsLambda.invokeFunction(
        functionName = envConfig.virology_scheduled_tokens_processing_function
    ).requireStatusCode(OK)

}

fun main() {
    BackgroundActivities(smoke.env.SmokeTests.loadConfig()).invokesBatchProcessing()
}
