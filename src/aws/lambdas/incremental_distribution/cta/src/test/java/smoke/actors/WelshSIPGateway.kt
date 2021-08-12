package smoke.actors

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.then
import smoke.env.EnvConfig
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.IsolationRequest
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse

class WelshSIPGateway(
    unauthedClient: HttpHandler,
    private val envConfig: EnvConfig
) {
    private val authedClient = SetAuthHeader(envConfig.auth_headers.isolationPayment).then(unauthedClient)

    fun consumeToken(ipcToken: IpcTokenId, status: Status): IsolationResponse =
        authedClient(
            Request(POST, envConfig.isolation_payment_consume_endpoint)
                .header("Content-Type", ContentType.APPLICATION_JSON.value)
                .body(Json.toJson(IsolationRequest(ipcToken)))
        ).requireStatusCode(status).deserializeOrThrow()

    fun verifyToken(ipcToken: IpcTokenId, status: Status): IsolationResponse = authedClient(
        Request(POST, envConfig.isolation_payment_verify_endpoint)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(Json.toJson(IsolationRequest(ipcToken)))
    ).requireStatusCode(status).deserializeOrThrow()
}
