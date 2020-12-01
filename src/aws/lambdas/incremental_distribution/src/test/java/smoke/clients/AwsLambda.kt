package smoke.clients

import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model.*

object AwsLambda {

    fun updateLambdaEnvVar(functionName: String, envVar: Pair<String, String>): UpdateFunctionConfigurationResult {
        val awsLambdaClient = AWSLambdaClientBuilder.standard().build()

        val configurationResponse = awsLambdaClient.getFunctionConfiguration(
            GetFunctionConfigurationRequest().withFunctionName(functionName)
        )

        val environment = configurationResponse.environment
        environment.variables[envVar.first] = envVar.second

        val updateRequest = UpdateFunctionConfigurationRequest()
            .withFunctionName(functionName)
            .withEnvironment(Environment().withVariables(environment.variables))

        return awsLambdaClient.updateFunctionConfiguration(updateRequest)
    }

    fun readLambdaEnvVar(functionName: String, envVar: String): String {
        val awsLambdaClient = AWSLambdaClientBuilder.standard().build()

        val configurationResponse = awsLambdaClient.getFunctionConfiguration(
            GetFunctionConfigurationRequest().withFunctionName(functionName)
        )

        return configurationResponse.environment.variables[envVar]!!
    }

    fun invokeFunction(functionName: String, payload: String? = null): InvokeResult {
        val awsLambdaClient = AWSLambdaClientBuilder.standard().build()
        val invokeRequest = InvokeRequest().withFunctionName(functionName)
        return if (payload == null) {
            awsLambdaClient.invoke(invokeRequest)
        } else {
            awsLambdaClient.invoke(invokeRequest.withPayload(payload))
        }
    }

    fun enableMaintenanceMode(lambdaFunctionName: String) {
        setMaintenanceMode(lambdaFunctionName, true)
    }

    fun disableMaintenanceMode(lambdaFunctionName: String) {
        setMaintenanceMode(lambdaFunctionName, false)
    }

    private fun setMaintenanceMode(lambdaFunctionName: String, value: Boolean) {
        val envVarName = "MAINTENANCE_MODE"
        val result = updateLambdaEnvVar(
            lambdaFunctionName,
            envVarName to "$value"
        )
        val updatedEnvVar = result.environment.variables[envVarName]
        if (updatedEnvVar != "$value")
            throw IllegalStateException("Expected env var: $envVarName to be updated but it was not.")
    }

    private fun setTokenCreationEnabled(lambdaFunctionName: String, value: Boolean) {
        val envVarName = "TOKEN_CREATION_ENABLED"
        val result = updateLambdaEnvVar(
            lambdaFunctionName,
            envVarName to "$value"
        )
        val updatedEnvVar = result.environment.variables[envVarName]
        if (updatedEnvVar != "$value")
            throw IllegalStateException("Expected env var: $envVarName to be updated but it was not.")
    }

    fun getTokenCreationEnabled(lambdaFunctionName: String): Boolean {
        val envVarName = "TOKEN_CREATION_ENABLED"
        return readLambdaEnvVar(
            lambdaFunctionName,
            envVarName
        ).toBoolean()
    }

    fun flipTokenCreationEnabled(lambdaFunctionName: String) {
        if (getTokenCreationEnabled(lambdaFunctionName)) {
            disableTokenCreationEnabled(lambdaFunctionName)
        } else {
            enableTokenCreationEnabled(lambdaFunctionName)

        }
    }

    fun enableTokenCreationEnabled(lambdaFunctionName: String) {
        setTokenCreationEnabled(lambdaFunctionName, true)
    }

    fun disableTokenCreationEnabled(lambdaFunctionName: String) {
        setTokenCreationEnabled(lambdaFunctionName, false)
    }

}