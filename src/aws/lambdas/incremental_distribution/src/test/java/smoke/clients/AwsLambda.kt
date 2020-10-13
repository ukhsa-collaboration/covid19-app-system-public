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

    fun readLambdaEnvVar(functionName: String, envVar: String): String{
        val awsLambdaClient = AWSLambdaClientBuilder.standard().build()

        val configurationResponse = awsLambdaClient.getFunctionConfiguration(
            GetFunctionConfigurationRequest().withFunctionName(functionName)
        )

        return configurationResponse.environment.variables[envVar]!!
    }

    fun invokeFunction(functionName: String): InvokeResult {
        val awsLambdaClient = AWSLambdaClientBuilder.standard().build()
        val invokeRequest = InvokeRequest().withFunctionName(functionName)
        return awsLambdaClient.invoke(invokeRequest)
    }

    fun enableMaintenanceMode(lambdaFunctionName: String) {
        setMaintenanceMode(lambdaFunctionName, true)
    }

    fun disableMaintenanceMode(lambdaFunctionName: String) {
        setMaintenanceMode(lambdaFunctionName, false)
    }

    private fun setMaintenanceMode(lambdaFunctionName: String, value: Boolean) {
        val envVarName = "MAINTENANCE_MODE"
        val result = AwsLambda.updateLambdaEnvVar(
            lambdaFunctionName,
            envVarName to "$value"
        )
        val updatedEnvVar = result.environment.variables[envVarName]
        if (updatedEnvVar != "$value")
            throw IllegalStateException("Expected env var: $envVarName to be updated but it was not.")
    }

}