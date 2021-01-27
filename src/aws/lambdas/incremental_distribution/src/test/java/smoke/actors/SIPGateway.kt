package smoke.actors

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.http4k.asString
import org.http4k.unquoted
import smoke.clients.AwsLambda
import smoke.env.EnvConfig
import uk.nhs.nhsx.core.Jackson

class SIPGateway(private val envConfig: EnvConfig) {

    fun consumesIpcToken(ipcToken: IpcToken): Map<String, String> {
        val consumePayload = lambdaCall(
            envConfig.isolationPaymentConsumeLambdaFunctionName,
            """{ "contractVersion": 1, "ipcToken": "${ipcToken.value}" }"""
        )
        assertThat(consumePayload["ipcToken"]).isEqualTo(ipcToken.value)
        return consumePayload
    }

    fun verifiesIpcToken(ipcToken: IpcToken): Map<String, String> {
        val verifyPayload = lambdaCall(
            envConfig.isolationPaymentVerifyLambdaFunctionName,
            """{ "contractVersion": 1, "ipcToken": "${ipcToken.value}" }"""
        )
        assertThat(verifyPayload["ipcToken"]).isEqualTo(ipcToken.value)
        return verifyPayload
    }
}

private fun lambdaCall(functionName: String, payload: String): Map<String, String> {
    val result = AwsLambda.invokeFunction(functionName, payload)
    return Jackson.readJson(result.payload.asString().unquoted(), object : TypeReference<Map<String, String>>() {})
}
