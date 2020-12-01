package smoke.clients

import org.apache.logging.log4j.LogManager
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import smoke.env.EnvConfig
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource.*
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponse
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse

class VirologyClient(private val client: JavaHttpClient,
                     private val config: EnvConfig) {


    companion object {
        private val logger = LogManager.getLogger(VirologyClient::class.java)

        fun baseUrlFrom(config: EnvConfig) = config.virologyKitEndpoint

        fun npexUploadEndpoint(config: EnvConfig) = config.test_results_npex_upload_endpoint
        fun fioranoUploadEndpoint(config: EnvConfig) = config.test_results_fiorano_upload_endpoint
        fun engTokenGenUploadEndpoint(config: EnvConfig) = config.engTokenGenUploadEndpoint
        fun wlsTokenGenUploadEndpoint(config: EnvConfig) = config.wlsTokenGenUploadEndpoint
        fun lfdTokenGenUploadEndpoint(config: EnvConfig) = config.lfdTokenGenUploadEndpoint
    }

    fun orderTest(): VirologyOrderResponse {
        logger.info("orderTest")

        val uri = "${baseUrlFrom(config)}/home-kit/order"

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)

        return client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }

    fun retrieveTestResult(pollingToken: String): VirologyLookupResponse {
        logger.info("retrieveTestResult")

        return retrieveVirologyResultFor(pollingToken)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }

    fun checkTestResultNotAvailableYet(pollingToken: String) {
        logger.info("checkTestResultNotPresentFor")

        retrieveVirologyResultFor(pollingToken)
            .requireStatusCode(Status.NO_CONTENT)
            .requireSignatureHeaders()
            .requireNoPayload()
    }

    private fun retrieveVirologyResultFor(pollingToken: String): Response {
        val uri = "${baseUrlFrom(config)}/results"

        val payload = """
            {
              "testResultPollingToken": "$pollingToken"
            }
        """

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(payload)

        return client(request)
    }

    fun exchangeCtaToken(virologyTokenGenResponse: VirologyTokenGenResponse): CtaExchangeResult {
        logger.info("exchangeCtaToken")
        val uri = "${baseUrlFrom(config)}/cta-exchange"
        val payload = """
            {
              "ctaToken": "${virologyTokenGenResponse.ctaToken}"
            }
        """
        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.mobile)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(payload)

        val response = client(request)

        if (response.status.code == 200) {
            return CtaExchangeResult.Available(response.deserializeOrThrow())
        } else if (response.status.code == 404) {
            return CtaExchangeResult.NotFound()
        }

        throw RuntimeException("Unhandled response")

    }

    fun uploadTestResult(virologyOrderResponse: VirologyOrderResponse, testResult: String) {
        logger.info("uploadTestResult")

        sendVirologyResults(virologyOrderResponse.tokenParameterValue, testResult)
            .requireStatusCode(Status.ACCEPTED)
            .requireBodyText("successfully processed")
    }

    fun uploadTestResultWithCheckingConflict(ctaToken: String, testResult: String) {
        logger.info("uploadTestResultWithCheckingConflict")

        sendVirologyResults(ctaToken, testResult)
            .requireStatusCode(Status.CONFLICT)
            .requireNoPayload()
    }

    private fun sendVirologyResults(ctaToken: String, testResult: String): Response {
        val uri = npexUploadEndpoint(config)

        val payload = """
                {
                  "ctaToken": "$ctaToken",
                  "testEndDate": "2020-04-23T00:00:00Z",
                  "testResult": "$testResult"
                }
            """

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.testResultUpload)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(payload)

        return client(request)
    }

    fun ctaTokenGen(testResult: String,
                    testEndDate: String = "2020-04-23T00:00:00Z",
                    source: VirologyTokenExchangeSource = Eng): VirologyTokenGenResponse {
        logger.info("ctaTokenGen")

        val uri = when (source) {
            Eng -> engTokenGenUploadEndpoint(config)
            Wls -> wlsTokenGenUploadEndpoint(config)
            Lfd -> lfdTokenGenUploadEndpoint(config)
        }

        val payload = """
            {
              "testEndDate": "$testEndDate",
              "testResult": "$testResult"
            }
        """

        val request = Request(Method.POST, uri)
            .header("Authorization", config.authHeaders.testResultUpload)
            .header("Content-Type", ContentType.APPLICATION_JSON.value)
            .body(payload)

        val response = client(request)

        return response
            .requireStatusCode(Status.OK)
            .deserializeOrThrow()
    }
}