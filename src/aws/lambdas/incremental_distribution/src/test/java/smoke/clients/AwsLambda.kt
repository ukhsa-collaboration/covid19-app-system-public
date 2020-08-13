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

    fun invokeFunction(functionName: String): InvokeResult {
        val awsLambdaClient = AWSLambdaClientBuilder.standard().build()
        val invokeRequest = InvokeRequest().withFunctionName(functionName)
        return awsLambdaClient.invoke(invokeRequest)
    }

}