package smoke.actors

import org.assertj.core.api.Assertions.assertThat
import org.http4k.format.Jackson
import org.http4k.unquoted
import smoke.clients.AwsLambda
import smoke.env.EnvConfig
import uk.nhs.nhsx.domain.IpcTokenId

class SIPGateway(private val envConfig: EnvConfig) {

    fun consumesIpcToken(ipcToken: IpcTokenId): Map<String, String> {
        val consumePayload = lambdaCall(
            envConfig.isolation_payment_consume_lambda_function_name,
            """{ "contractVersion": 1, "ipcToken": "${ipcToken.value}" }"""
        )
        assertThat(consumePayload["ipcToken"]).isEqualTo(ipcToken.value)
        return consumePayload
    }

    fun verifiesIpcToken(ipcToken: IpcTokenId): Map<String, String> {
        val verifyPayload = lambdaCall(
            envConfig.isolation_payment_verify_lambda_function_name,
            """{ "contractVersion": 1, "ipcToken": "${ipcToken.value}" }"""
        )
        assertThat(verifyPayload["ipcToken"]).isEqualTo(ipcToken.value)
        return verifyPayload
    }
}

private fun lambdaCall(functionName: String, payload: String): Map<String, String> {
    val result = AwsLambda.invokeFunction(functionName, payload)
    return Jackson.asA(result.payload().asUtf8String().unquoted())
}
