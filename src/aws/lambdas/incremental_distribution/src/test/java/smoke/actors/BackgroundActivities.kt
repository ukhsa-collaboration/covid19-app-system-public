package smoke.actors

import org.http4k.core.Status.Companion.OK
import smoke.clients.AwsLambda
import smoke.env.EnvConfig

class BackgroundActivities(private val envConfig: EnvConfig) {
    fun invokesBatchProcessing() = AwsLambda.invokeFunction(envConfig.diagnosisKeysProcessingFunction)
        .requireStatusCode(OK)
        .requireBodyText("\"success\"")
}
