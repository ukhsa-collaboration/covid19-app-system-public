package smoke.actors

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.then
import smoke.env.EnvConfig
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.isolationpayment.model.IsolationRequest
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse
import uk.nhs.nhsx.virology.IpcTokenId

class WelshSIPGateway(
    unauthedClient: HttpHandler,
    private val envConfig: EnvConfig
) {
    private val authedClient = SetAuthHeader(envConfig.authHeaders.isolationPayment).then(unauthedClient)

    fun consumeToken(ipcToken: IpcTokenId, status: Status): IsolationResponse {
        val consumedToken = authedClient(
            Request(Method.POST, envConfig.isolationPaymentConsumeEndpoint)
                .header("Content-Type", ContentType.APPLICATION_JSON.value)
                .body(Jackson.toJson(IsolationRequest(ipcToken)))
        )
            .requireStatusCode(status)
            .deserializeOrThrow<IsolationResponse>()
        return consumedToken
    }

    fun verifyToken(ipcToken: IpcTokenId, status: Status): IsolationResponse {
        val verifiedToken = authedClient(
            Request(Method.POST, envConfig.isolationPaymentVerifyEndpoint)
                .header("Content-Type", ContentType.APPLICATION_JSON.value)
                .body(Jackson.toJson(IsolationRequest(ipcToken)))
        )
            .requireStatusCode(status)
            .deserializeOrThrow<IsolationResponse>()
        return verifiedToken
    }
}
