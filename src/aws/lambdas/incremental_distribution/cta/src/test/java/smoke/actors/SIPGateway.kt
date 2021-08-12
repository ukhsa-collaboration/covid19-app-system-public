package smoke.actors

import org.http4k.format.Jackson
import org.http4k.unquoted
import smoke.clients.AwsLambda
import smoke.env.EnvConfig
import strikt.api.expectThat
import strikt.assertions.getValue
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.domain.IpcTokenId

class SIPGateway(private val envConfig: EnvConfig) {

    fun consumesIpcToken(ipcToken: IpcTokenId): Map<String, String> {
        val consumePayload = lambdaCall(
            envConfig.isolation_payment_consume_lambda_function_name,
            """{ "contractVersion": 1, "ipcToken": "${ipcToken.value}" }"""
        )
        expectThat(consumePayload).getValue("ipcToken").isEqualTo(ipcToken.value)
        return consumePayload
    }

    fun verifiesIpcToken(ipcToken: IpcTokenId): Map<String, String> {
        val verifyPayload = lambdaCall(
            envConfig.isolation_payment_verify_lambda_function_name,
            """{ "contractVersion": 1, "ipcToken": "${ipcToken.value}" }"""
        )
        expectThat(verifyPayload).getValue("ipcToken").isEqualTo(ipcToken.value)
        return verifyPayload
    }
}

private fun lambdaCall(functionName: String, payload: String): Map<String, String> {
    val result = AwsLambda.invokeFunction(functionName, payload)
    return Jackson.asA(result.payload().asUtf8String().unquoted())
}
