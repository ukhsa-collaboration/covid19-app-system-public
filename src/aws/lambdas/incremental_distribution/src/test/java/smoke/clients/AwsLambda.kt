package smoke.clients

import org.http4k.aws.AwsSdkClient
import org.http4k.client.JavaHttpClient
import org.http4k.filter.debug
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.Environment
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationResponse
import java.nio.charset.Charset


object AwsLambda {

    private val client = LambdaClient.builder().httpClient(AwsSdkClient(JavaHttpClient().debug())).build()

    fun updateLambdaEnvVar(functionName: String, envVar: Pair<String, String>): UpdateFunctionConfigurationResponse {
        val awsLambdaClient = client

        val configurationResponse = awsLambdaClient.getFunctionConfiguration(
            GetFunctionConfigurationRequest.builder().functionName(functionName).build()
        )

        val environment = configurationResponse.environment()

        val variables = Environment.builder()
            .variables(environment.variables() + (envVar.first to envVar.second))
        val updateRequest = UpdateFunctionConfigurationRequest.builder()
            .functionName(functionName)
            .environment(variables.build()).build()

        return awsLambdaClient.updateFunctionConfiguration(updateRequest)
    }

    fun readLambdaEnvVar(functionName: String, envVar: String): String {
        val awsLambdaClient = client

        val configurationResponse = awsLambdaClient.getFunctionConfiguration(
            GetFunctionConfigurationRequest.builder().functionName(functionName).build()
        )

        return configurationResponse.environment().variables()[envVar]!!
    }

    fun invokeFunction(functionName: String, payload: String? = null): InvokeResponse {
        val awsLambdaClient = client
        val invokeRequest = InvokeRequest.builder().functionName(functionName)
        return if (payload == null) {
            awsLambdaClient.invoke(invokeRequest.build())
        } else {
            awsLambdaClient.invoke(
                invokeRequest.payload(SdkBytes.fromString(payload, Charset.defaultCharset())).build()
            )
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
        val updatedEnvVar = result.environment().variables()[envVarName]
        if (updatedEnvVar != "$value")
            throw IllegalStateException("Expected env var: $envVarName to be updated but it was not.")
    }

    private fun setTokenCreationEnabled(lambdaFunctionName: String, value: Boolean) {
        val envVarName = "TOKEN_CREATION_ENABLED"
        val result = updateLambdaEnvVar(
            lambdaFunctionName,
            envVarName to "$value"
        )
        val updatedEnvVar = result.environment().variables()[envVarName]
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
