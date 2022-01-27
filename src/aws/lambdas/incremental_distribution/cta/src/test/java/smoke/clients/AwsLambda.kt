package smoke.clients

import org.awaitility.Awaitility.await
import org.http4k.aws.AwsSdkClient
import org.http4k.client.JavaHttpClient
import org.http4k.filter.debug
import smoke.clients.AwsLambda.MaintenanceModeFlag.DISABLED
import smoke.clients.AwsLambda.MaintenanceModeFlag.ENABLED
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.Environment
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationResponse
import java.time.Duration
import kotlin.text.Charsets.UTF_8

object AwsLambda {

    private val client = LambdaClient.builder()
        .httpClient(AwsSdkClient(JavaHttpClient().debug()))
        .build()

    fun enableMaintenanceMode(lambdaFunctionName: String) {
        setMaintenanceMode(lambdaFunctionName, ENABLED)
    }

    fun disableMaintenanceMode(lambdaFunctionName: String) {
        setMaintenanceMode(lambdaFunctionName, DISABLED)
    }

    fun updateLambdaEnvVar(
        functionName: String,
        envVar: Pair<String, String>
    ): UpdateFunctionConfigurationResponse {
        val environment = client.getFunctionConfiguration(
            GetFunctionConfigurationRequest.builder()
                .functionName(functionName)
                .build()
        ).environment()

        val variables = Environment.builder()
            .variables(environment.variables() + (envVar.first to envVar.second))

        return client.updateFunctionConfiguration(
            UpdateFunctionConfigurationRequest.builder()
                .functionName(functionName)
                .environment(variables.build())
                .build()
        )
    }

    fun invokeFunction(
        functionName: String,
        payload: String? = null
    ): InvokeResponse = when (payload) {
        null -> client.invoke(
            InvokeRequest.builder()
                .functionName(functionName)
                .build()
        )
        else -> client.invoke(
            InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromString(payload, UTF_8))
                .build()
        )
    }

    private enum class MaintenanceModeFlag {
        ENABLED, DISABLED
    }

    private fun setMaintenanceMode(
        lambdaFunctionName: String,
        flag: MaintenanceModeFlag
    ) {
        await()
            .atMost(Duration.ofMinutes(1))
            .pollInterval(Duration.ofSeconds(1))
            .ignoreExceptions()
            .untilAsserted {
                val value = flag == ENABLED
                val result = updateLambdaEnvVar(lambdaFunctionName, "MAINTENANCE_MODE" to "$value")
                val updatedEnvVar = result.environment().variables()["MAINTENANCE_MODE"]
                if (updatedEnvVar != "$value") error("Expected env var: MAINTENANCE_MODE to be updated but it was not.")
            }
    }
}
