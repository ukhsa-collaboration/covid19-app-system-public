package smoke.actors

import com.natpryce.hamkrest.containsSubstring
import org.http4k.core.Status.Companion.OK
import smoke.clients.AwsLambda
import smoke.env.EnvConfig

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
}

fun main() {
    BackgroundActivities(smoke.env.SmokeTests.loadConfig()).invokesBatchProcessing()
}
