package smoke.actors

import com.natpryce.hamkrest.containsSubstring
import org.http4k.core.Status.Companion.OK
import org.joda.time.DateTime
import smoke.clients.AwsLambda
import smoke.env.EnvConfig
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import java.time.Instant

class BackgroundActivities(private val envConfig: EnvConfig) {

    fun invokesBatchProcessing() {

        AwsLambda.invokeFunction(envConfig.diagnosisKeysProcessingFunction)
            .requireStatusCode(OK)
            .requireBodyText(containsSubstring("KeysDistributed"))

        AwsLambda.invokeFunction(envConfig.federationKeysProcessingDownloadFunction)
            .requireStatusCode(OK)
            .requireBodyText(containsSubstring("InteropConnectorDownloadStats"))

        AwsLambda.invokeFunction(envConfig.federationKeysProcessingUploadFunction)
            .requireStatusCode(OK)
            .requireBodyText(containsSubstring("InteropConnectorUploadStats"))
    }

    fun invokeAnalyticsLogs(scheduledEventTime: Instant, functionName: String): InvokeResponse {
        return AwsLambda.invokeFunction(functionName, """{ "time": "${DateTime(scheduledEventTime.toEpochMilli())}" }""")
            .requireStatusCode(OK)
    }
}

fun main() {
    BackgroundActivities(smoke.env.SmokeTests.loadConfig()).invokesBatchProcessing()
}
