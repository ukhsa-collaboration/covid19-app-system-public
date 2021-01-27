package smoke.actors

import batchZipCreation.Exposure
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isNullOrEmptyString
import org.assertj.core.api.Assertions.assertThat
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import smoke.actors.ApiVersion.V2
import smoke.actors.MobileOS.Android
import smoke.actors.MobileOS.iOS
import smoke.data.DiagnosisKeyData.createKeysPayload
import smoke.data.DiagnosisKeyData.createKeysPayloadWithOnsetDays
import smoke.env.EnvConfig
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetadata
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetrics
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsWindow
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.circuitbreakers.ResolutionResponse
import uk.nhs.nhsx.circuitbreakers.TokenResponse
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.SystemObjectMapper.MAPPER
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import uk.nhs.nhsx.testhelper.BatchExport
import uk.nhs.nhsx.testhelper.data.TestData.EXPOSURE_NOTIFICATION_CIRCUIT_BREAKER_PAYLOAD
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponse
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MobileApp(
    private val unauthedClient: HttpHandler,
    private val envConfig: EnvConfig,
    private val os: MobileOS = iOS,
    private val model: MobileDeviceModel? = null,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val authedClient = SetAuthHeader(envConfig.authHeaders.mobile).then(unauthedClient)
    val exposureCircuitBreaker = CircuitBreaker(authedClient, envConfig.exposureNotificationCircuitBreakerEndpoint, EXPOSURE_NOTIFICATION_CIRCUIT_BREAKER_PAYLOAD)
    val venueCircuitBreaker = CircuitBreaker(authedClient, envConfig.riskyVenuesCircuitBreakerEndpoint)

    private val diagnosisKeys = DiagnosisKeysDownload(unauthedClient, envConfig)
    private var orderedTest: VirologyOrderResponse? = null

    fun pollRiskyPostcodes(version: ApiVersion): Map<String, Any> =
        MAPPER.readValue(when (version) {
            ApiVersion.V1 -> getStaticContent(envConfig.postDistrictsDistUrl)
            V2 -> getStaticContent(envConfig.postDistrictsDistUrl + "-v2")
        }, object : TypeReference<Map<String, Any>>() {})

    fun pollRiskyVenues() = deserializeWithOptional(getStaticContent(envConfig.riskyVenuesDistUrl))!!

    fun pollAvailability() = when (os) {
        iOS -> getStaticContent(envConfig.availabilityIosDistUrl)
        Android -> getStaticContent(envConfig.availabilityAndroidDistUrl)
    }

    fun pollExposureConfig() = getStaticContent(envConfig.exposureConfigurationDistUrl)
    fun pollSelfIsolation() = getStaticContent(envConfig.selfIsolationDistUrl)
    fun pollRiskyVenuesMessages() = getStaticContent(envConfig.riskyVenuesMessagesDownloadEndpoint)
    fun pollSymptomaticQuestionnaire() = getStaticContent(envConfig.symptomaticQuestionnaireDistUrl)

    fun exchange(ctaToken: CtaToken, version: ApiVersion): CtaExchangeResult {
        val (uri, payload) = when (version) {
            ApiVersion.V1 -> Pair(
                "${envConfig.virologyKitEndpoint}/cta-exchange",
                """
                    {
                      "ctaToken": "${ctaToken.value}"
                    }
                """
            )
            V2 -> Pair(
                "${envConfig.virologyKitEndpoint}/v2/cta-exchange",
                """
                    {
                      "ctaToken": "${ctaToken.value}",
                      "country": "England"
                    }
                """
            )
        }

        val request = Request(POST, uri)
            .header("Content-Type", APPLICATION_JSON.value)
            .body(payload)
        val response = authedClient(request)
        return when (response.status.code) {
            200 -> CtaExchangeResult.Available(response.deserializeOrThrow())
            404 -> CtaExchangeResult.NotFound()
            else -> throw RuntimeException("Unhandled response")
        }
    }

    fun submitAnalyticsKeys(window: AnalyticsWindow, metrics: AnalyticsMetrics): Status {
        val metadata = when (os) {
            Android -> AnalyticsMetadata("AL1", model?.value ?: "HUAWEI-smoke-test", "29", "3.0", "E07000240")
            iOS -> AnalyticsMetadata("AL1", model?.value ?: "iPhone-smoke-test", "iPhone OS 13.5.1 (17F80)", "3.0", "E07000240")
        }
        return authedClient(Request(POST, envConfig.analyticsSubmissionEndpoint)
            .header("Content-Type", ContentType("text/json").value)
            .body(Jackson.toJson(ClientAnalyticsSubmissionPayload(window, metadata, metrics, false)))
        ).status
    }

    fun orderTest(version: ApiVersion): VirologyOrderResponse {
        val url = when (version) {
            ApiVersion.V1 -> "/home-kit/order"
            V2 -> "/v2/order"
        }
        orderedTest = authedClient(Request(POST, "${envConfig.virologyKitEndpoint}$url"))
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow<VirologyOrderResponse>()
        return orderedTest!!
    }

    fun pollForCompleteTestResult(version: ApiVersion): VirologyLookupResponse = orderedTest?.let {
        retrieveTestResult(TestResultPollingToken(it.testResultPollingToken), version)
    } ?: error("no test ordered!")

    fun pollForIncompleteTestResult(version: ApiVersion) = orderedTest?.let {
        checkTestResultNotAvailableYet(TestResultPollingToken(it.testResultPollingToken), version)
    } ?: error("no test ordered!")

    fun pollForNotFoundTestResult(version: ApiVersion) = orderedTest?.let {
        checkTestResultNotFound(TestResultPollingToken(it.testResultPollingToken), version)
    } ?: error("no test ordered!")

    fun submitAnalyticEvents(json: String): Response = authedClient(Request(POST, envConfig.analyticsEventsSubmissionEndpoint)
        .header("Content-Type", ContentType("text/json").value)
        .body(json)
    )

    fun submitKeys(diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
                   encodedSubmissionKeys: List<String>): ClientTemporaryExposureKeysPayload {
        val payload = createKeysPayload(diagnosisKeySubmissionToken, encodedSubmissionKeys, clock)
        sendTempExposureKeys(payload)
        return payload
    }

    fun submitKeysWithOnsetDays(diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken, encodedSubmissionKeys: List<String>): ClientTemporaryExposureKeysPayload {
        val payload = createKeysPayloadWithOnsetDays(diagnosisKeySubmissionToken, encodedSubmissionKeys, clock)
        sendTempExposureKeys(payload)
        return payload
    }

    fun createIsolationToken(country: UserCountry) = authedClient(Request(POST, envConfig.isolationPaymentCreateEndpoint)
        .header("Content-Type", APPLICATION_JSON.value)
        .body(Jackson.toJson(TokenGenerationRequest(country.value))))
        .requireStatusCode(Status.CREATED)
        .requireSignatureHeaders()
        .deserializeOrThrow<TokenGenerationResponse>()

    fun updateIsolationToken(ipcToken: IpcToken, riskyEncounterDateString: String, isolationPeriodEndDateString: String) {
        val isolationTokenUpdateResponse = authedClient(submitIsolationTokenUpdateRequest(TokenUpdateRequest(ipcToken.value, riskyEncounterDateString, isolationPeriodEndDateString)
        ))
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow<TokenUpdateResponse>()
        assertThat(isolationTokenUpdateResponse.websiteUrlWithQuery).endsWith(ipcToken.value)
    }

    fun updateIsolationTokenInvalid(ipcToken: IpcToken, riskyEncounterDateString: String, isolationPeriodEndDateString: String) =
        authedClient(submitIsolationTokenUpdateRequest(TokenUpdateRequest(ipcToken.value, riskyEncounterDateString, isolationPeriodEndDateString)
        ))
            .requireStatusCode(Status.BAD_REQUEST)
            .requireSignatureHeaders()
            .toString()

    private fun retrieveTestResult(pollingToken: TestResultPollingToken, version: ApiVersion): VirologyLookupResponse =
        retrieveVirologyResultFor(pollingToken, version)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()

    private fun checkTestResultNotAvailableYet(pollingToken: TestResultPollingToken, version: ApiVersion) {
        retrieveVirologyResultFor(pollingToken, version)
            .requireStatusCode(Status.NO_CONTENT)
            .requireSignatureHeaders()
            .requireNoPayload()
    }

    private fun checkTestResultNotFound(pollingToken: TestResultPollingToken, version: ApiVersion) {
        retrieveVirologyResultFor(pollingToken, version)
            .requireStatusCode(Status.NOT_FOUND)
            .requireSignatureHeaders()
            .requireBodyText("Test result lookup submitted for unknown testResultPollingToken")
    }

    private fun retrieveVirologyResultFor(pollingToken: TestResultPollingToken,
                                          version: ApiVersion
    ): Response {
        val (uri, payload) = when (version) {
            ApiVersion.V1 -> Pair(
                "${envConfig.virologyKitEndpoint}/results",
                """
                    {
                      "testResultPollingToken": "${pollingToken.value}"
                    }
                """
            )
            V2 -> Pair(
                "${envConfig.virologyKitEndpoint}/v2/results",
                """
                    {
                      "testResultPollingToken": "${pollingToken.value}", 
                      "country": "England"
                    }
                """
            )
        }

        return authedClient(Request(POST, uri)
            .header("Content-Type", APPLICATION_JSON.value)
            .body(payload))
    }

    private fun getStaticContent(uri: String) = unauthedClient(Request(Method.GET, uri))
        .requireStatusCode(Status.OK)
        .requireSignatureHeaders()
        .requireJsonContentType()
        .bodyString()

    private fun submitIsolationTokenUpdateRequest(updateRequest: TokenUpdateRequest): Request =
        Request(POST, envConfig.isolationPaymentUpdateEndpoint)
            .header("Content-Type", "application/json")
            .body(Jackson.toJson(updateRequest))


    private fun sendTempExposureKeys(payload: ClientTemporaryExposureKeysPayload) {
        val request = Request(POST, envConfig.diagnosisKeysSubmissionEndpoint)
            .header("Content-Type", APPLICATION_JSON.value)
            .body(Jackson.toJson(payload))

        authedClient(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .requireNoPayload()
    }

    fun getLatestTwoHourlyTekExport() = diagnosisKeys.getLatestTwoHourlyTekExport()
    fun getDailyTekExport(filename: String) = diagnosisKeys.getDailyTekExport(filename)
    fun getTwoHourlyTekExport(filename: String) = diagnosisKeys.getTwoHourlyTekExport(filename)
}

class CircuitBreaker(private val authedClient: HttpHandler,
                     private val baseUrl: String,
                     private val payload: String = "") {


    fun request(): TokenResponse =
        authedClient(Request(POST, "$baseUrl/request")
            .header("Content-Type", APPLICATION_JSON.value)
            .body(payload)
        )
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()

            .deserializeOrThrow()

    fun resolve(tokenResponse: TokenResponse): ResolutionResponse =
        authedClient(Request(Method.GET, "$baseUrl/resolution/${tokenResponse.approvalToken}")
            .header("Content-Type", APPLICATION_JSON.value))
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()


    fun requestAndApproveCircuitBreak() {
        val tokenResponse = request()
        assertThat(tokenResponse.approval, equalTo("yes"))
        assertThat(tokenResponse.approvalToken, !isNullOrEmptyString)

        assertThat(resolve(tokenResponse).approval, equalTo("yes"))
    }
}

private fun deserializeWithOptional(staticContentRiskyVenues: String) =
    LEINIENT_MAPPER.readValue(staticContentRiskyVenues, HighRiskVenues::class.java)

private val LEINIENT_MAPPER = ObjectMapper()
    .deactivateDefaultTyping()
    .registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
    .registerModule(Jdk8Module())


class DiagnosisKeysDownload(private val unauthedClient: HttpHandler, private val envConfig: EnvConfig) {
    fun getLatestTwoHourlyTekExport(): Exposure.TemporaryExposureKeyExport {
        val dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"))
        val twoHourlyFilename = currentTwoHourlyWindowFilename(dateTime)
        return getTwoHourlyTekExport(twoHourlyFilename)
    }

    fun getTwoHourlyTekExport(twoHourlyFilename: String): Exposure.TemporaryExposureKeyExport {
        val request = Request(Method.GET, "${envConfig.diagnosisKeysDist2hourlyEndpoint}/$twoHourlyFilename")

        val response = getCloudfrontContentRetrying(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .requireZipContentType()

        return BatchExport.tekExportFrom(response.body.stream)
    }

    fun getDailyTekExport(dailyFilename: String): Exposure.TemporaryExposureKeyExport {
        val uri = "${envConfig.diagnosisKeysDistributionDailyEndpoint}/$dailyFilename"
        val request = Request(Method.GET, uri)

        val response =
            getCloudfrontContentRetrying(request)
                .requireStatusCode(Status.OK)
                .requireSignatureHeaders()
                .requireZipContentType()

        return BatchExport.tekExportFrom(response.body.stream)
    }

    private fun currentTwoHourlyWindowFilename(dateTime: LocalDateTime): String {
        val twoHourlyWindow = when {
            dateTime.hour % 2 == 0 -> dateTime.plusHours(2) // use next 2 hour window
            else -> dateTime.plusHours(1) // use current 2 hour window
        }

        val twoHourlyWindowStr = twoHourlyWindow
            .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
            .toString()

        return "$twoHourlyWindowStr.zip"
    }

    private fun getCloudfrontContentRetrying(request: Request): Response {
        var numberOfTries = 0L
        val maxRetries = 5
        val retryWaitDuration = Duration.ofSeconds(5)

        do {
            val response = unauthedClient(request)
            if (response.status == Status.OK) return response
            numberOfTries++
            val sleepDuration = Duration.ofSeconds(numberOfTries).plus(retryWaitDuration)

            if (numberOfTries <= maxRetries) Thread.sleep(sleepDuration.toMillis())
        } while (numberOfTries <= maxRetries)

        throw IllegalStateException("Tried to fetch cloudfront content but failed after $maxRetries attempts")
    }
}
