package smoke.clients

import org.apache.logging.log4j.LogManager
import org.http4k.core.*
import smoke.env.EnvConfig
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse

class IsolationPaymentClient(private val client: HttpHandler,
                             private val config: EnvConfig) {

    companion object {
        private val logger = LogManager.getLogger(IsolationPaymentClient::class.java)
    }

    fun submitIsolationTokenCreate(tokenGenerationRequest: TokenGenerationRequest): TokenGenerationResponse {
        logger.info("submitIsolationTokenCreate")

        val uri = config.isolationPaymentCreateEndpoint

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(Jackson.toJson(tokenGenerationRequest))

        return client(request)
            .requireStatusCode(Status.CREATED)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }
    fun submitIsolationTokenCreateTokenCreationDisabled(tokenGenerationRequest: TokenGenerationRequest): Response {
        logger.info("submitIsolationTokenCreate")

        val uri = config.isolationPaymentCreateEndpoint

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(Jackson.toJson(tokenGenerationRequest))

        return client(request)
            .requireStatusCode(Status.SERVICE_UNAVAILABLE)
            .requireSignatureHeaders()
    }

    fun submitIsolationTokenUpdate(updateRequest: TokenUpdateRequest): TokenUpdateResponse {
        logger.info("submitIsolationTokenUpdate")

        val uri = config.isolationPaymentUpdateEndpoint

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", "application/json")
            .body(Jackson.toJson(updateRequest))

        return client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }

    fun submitIsolationTokenUpdateTokenCreationDisabled(updateRequest: TokenUpdateRequest): Response {
        logger.info("submitIsolationTokenUpdate")

        val uri = config.isolationPaymentUpdateEndpoint

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", "application/json")
            .body(Jackson.toJson(updateRequest))

        return client(request)
            .requireStatusCode(Status.SERVICE_UNAVAILABLE)
            .requireSignatureHeaders()
    }
}