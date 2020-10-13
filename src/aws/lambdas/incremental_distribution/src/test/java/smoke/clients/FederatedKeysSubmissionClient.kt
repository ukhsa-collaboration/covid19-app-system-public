package smoke.clients

import org.http4k.core.Status
import smoke.clients.AwsLambda.invokeFunction
import smoke.clients.AwsLambda.readLambdaEnvVar
import smoke.env.EnvConfig

class FederatedKeysSubmissionClient(private val config: EnvConfig) {

    val functionName = config.federatedKeysProcessingFunction

    fun invokeFederatedKeysProcessing(){
        invokeFunction(functionName)
                .requireStatusCode(Status.OK)
                .requireBodyText("\"success\"")
    }

    fun invokeFederatedKeysDownload(){
        val downloadEnabledInitialValue = readLambdaEnvVar(functionName, "DOWNLOAD_ENABLED")
        val uploadEnabledInitialValue = readLambdaEnvVar(functionName, "UPLOAD_ENABLED")
        if (!downloadEnabledInitialValue.toBoolean()) {
            updateEnvVar("DOWNLOAD_ENABLED", true)
        }
        if (uploadEnabledInitialValue.toBoolean()) {
            updateEnvVar("UPLOAD_ENABLED", false)
        }
        invokeFederatedKeysProcessing()
        if (!downloadEnabledInitialValue.toBoolean()) {
            updateEnvVar("DOWNLOAD_ENABLED", false)
        }
        if (uploadEnabledInitialValue.toBoolean()) {
            updateEnvVar("UPLOAD_ENABLED", true)
        }
    }

    private fun updateEnvVar(envVar: String, value: Boolean){
        val result = AwsLambda.updateLambdaEnvVar(
                functionName,
                envVar to "$value"
        )
        val updatedEnvVar = result.environment.variables[envVar]
        if (updatedEnvVar != "$value")
            throw IllegalStateException("Expected env var: $envVar to be updated but it was not.")
    }
}