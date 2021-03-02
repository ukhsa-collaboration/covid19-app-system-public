package smoke.actors

import org.http4k.core.*
import smoke.env.EnvConfig
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.isolationpayment.model.IsolationRequest
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse

class WelshSIPGateway(private val unauthedClient: HttpHandler,
                      private val envConfig: EnvConfig) {
    private val authedClient = SetAuthHeader(envConfig.authHeaders.isolationPayment).then(unauthedClient)

    fun consumeToken(ipcToken: IpcToken, status: Status): IsolationResponse {
        var consumedToken = authedClient(Request(Method.POST, envConfig.isolationPaymentConsumeEndpoint)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(Jackson.toJson(IsolationRequest(ipcToken.value)))
        )
            .requireStatusCode(status)
            .deserializeOrThrow<IsolationResponse>()
        return consumedToken
    }

    fun verifyToken(ipcToken: IpcToken, status: Status): IsolationResponse {
        print(Jackson.toJson(IsolationRequest(ipcToken.value)))
        var verifiedToken = authedClient(Request(Method.POST, envConfig.isolationPaymentVerifyEndpoint)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(Jackson.toJson(IsolationRequest(ipcToken.value)))
        )
            .requireStatusCode(status)
            .deserializeOrThrow<IsolationResponse>()
        return verifiedToken
    }

}
