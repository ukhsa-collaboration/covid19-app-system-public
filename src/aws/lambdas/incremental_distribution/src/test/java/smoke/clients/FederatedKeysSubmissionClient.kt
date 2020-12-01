package smoke.clients

import org.http4k.core.Status
import smoke.clients.AwsLambda.invokeFunction
import smoke.clients.AwsLambda.readLambdaEnvVar
import smoke.env.EnvConfig

class FederatedKeysSubmissionClient(private val config: EnvConfig) {

    val uploadFunctionName = config.federationKeysProcessingUploadFunction
    val downloadFunctionName = config.federationKeysProcessingDownloadFunction

    fun invokeFederatedKeysProcessingForUpload(){
        invokeFunction(uploadFunctionName)
                .requireStatusCode(Status.OK)
                .requireBodyText("\"success\"")
    }

    fun invokeFederatedKeysProcessingForDownload(){
        invokeFunction(downloadFunctionName)
            .requireStatusCode(Status.OK)
            .requireBodyText("\"success\"")
    }

    fun invokeFederatedKeysDownload(){
        val downloadEnabledInitialValue = readLambdaEnvVar(downloadFunctionName, "DOWNLOAD_ENABLED")
        val uploadEnabledInitialValue = readLambdaEnvVar(uploadFunctionName, "UPLOAD_ENABLED")
        if (!downloadEnabledInitialValue.toBoolean()) {
            updateEnvVar("DOWNLOAD_ENABLED", downloadFunctionName,true)
        }
        if (uploadEnabledInitialValue.toBoolean()) {
            updateEnvVar("UPLOAD_ENABLED",uploadFunctionName, false)
        }
        invokeFederatedKeysProcessingForUpload()
        invokeFederatedKeysProcessingForDownload()
        if (!downloadEnabledInitialValue.toBoolean()) {
            updateEnvVar("DOWNLOAD_ENABLED", downloadFunctionName,false)
        }
        if (uploadEnabledInitialValue.toBoolean()) {
            updateEnvVar("UPLOAD_ENABLED", uploadFunctionName, true)
        }
    }

    private fun updateEnvVar(envVar: String, functionName: String, value: Boolean){
        val result = AwsLambda.updateLambdaEnvVar(
                functionName,
                envVar to "$value"
        )
        val updatedEnvVar = result.environment.variables[envVar]
        if (updatedEnvVar != "$value")
            throw IllegalStateException("Expected env var: $envVar to be updated but it was not.")
    }
}