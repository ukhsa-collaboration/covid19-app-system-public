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
import org.http4k.filter.debug
import smoke.actors.ApiVersion.V1
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
import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.SystemObjectMapper.MAPPER
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import uk.nhs.nhsx.testhelper.BatchExport
import uk.nhs.nhsx.testhelper.data.TestData.EXPOSURE_NOTIFICATION_CIRCUIT_BREAKER_PAYLOAD
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import uk.nhs.nhsx.virology.result.VirologyLookupResult
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MobileApp(private val unauthedClient: HttpHandler,
                private val envConfig: EnvConfig,
                private val os: MobileOS = iOS,
                private val appVersion: MobileAppVersion.Version = MobileAppVersion.Version(4, 4),
                private val model: MobileDeviceModel? = null,
                private val clock: Clock = Clock.systemDefaultZone()) {

    private val authedClient = SetAuthHeader(envConfig.authHeaders.mobile).then(unauthedClient)
    val exposureCircuitBreaker = CircuitBreaker(authedClient, envConfig.exposureNotificationCircuitBreakerEndpoint, EXPOSURE_NOTIFICATION_CIRCUIT_BREAKER_PAYLOAD)
    val venueCircuitBreaker = CircuitBreaker(authedClient, envConfig.riskyVenuesCircuitBreakerEndpoint)

    private val diagnosisKeys = DiagnosisKeysDownload(unauthedClient, envConfig, clock)
    private var orderedTest: VirologyOrderResponse? = null

    fun pollRiskyPostcodes(version: ApiVersion): Map<String, Any> =
        MAPPER.readValue(when (version) {
            ApiVersion.V1 -> getStaticContent(envConfig.postDistrictsDistUrl)
            ApiVersion.V2 -> getStaticContent(envConfig.postDistrictsDistUrl + "-v2")
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

    fun exchange(ctaToken: CtaToken): CtaExchangeResult {
        val uri = "${envConfig.virologyKitEndpoint}/cta-exchange"
        val payload = """
                {
                  "ctaToken": "${ctaToken.value}"
                }
            """
        val request = Request(POST, uri)
            .header("Content-Type", APPLICATION_JSON.value)
            .body(payload)
        val response = authedClient(request)
        return when (response.status.code) {
            200 -> CtaExchangeResult.Available(response.deserializeOrThrow())
            404 -> CtaExchangeResult.NotFound()
            else -> throw RuntimeException("Unhandled response " + response.status.code)
        }
    }

    fun submitAnalyticsKeys(window: AnalyticsWindow, metrics: AnalyticsMetrics): Status {
        val metadata = when (os) {
            Android -> AnalyticsMetadata("AL1", model?.value ?: "HUAWEI-smoke-test", "29", "3.0", "E07000240")
            iOS -> AnalyticsMetadata("AL1", model?.value
                ?: "iPhone-smoke-test", "iPhone OS 13.5.1 (17F80)", "3.0", "E07000240")
        }
        return authedClient(Request(POST, envConfig.analyticsSubmissionEndpoint)
            .header("Content-Type", ContentType("text/json").value)
            .body(Jackson.toJson(ClientAnalyticsSubmissionPayload(window, metadata, metrics, false)))
        ).status
    }

    fun orderTest(): VirologyOrderResponse {
        orderedTest = authedClient(Request(POST, "${envConfig.virologyKitEndpoint}/home-kit/order"))
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow<VirologyOrderResponse>()
        return orderedTest!!
    }

    fun registerTest(): VirologyOrderResponse {
        orderedTest = authedClient(Request(POST, "${envConfig.virologyKitEndpoint}/home-kit/register"))
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow<VirologyOrderResponse>()
        return orderedTest!!
    }

    fun submitAnalyticEvents(json: String): Response = authedClient(Request(POST, envConfig.analyticsEventsSubmissionEndpoint)
        .header("Content-Type", ContentType("text/json").value)
        .body(json)
    )

    fun emptySubmission() {
        authedClient(Request(POST, envConfig.emptySubmissionEndpoint))
    }

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

    fun updateIsolationToken(ipcToken: IpcToken, riskyEncounterDate: OffsetDateTime, isolationPeriodEndDate: OffsetDateTime) {
        val riskyEncounterDateString = riskyEncounterDate.format(DateFormatValidator.formatter);
        val isolationPeriodEndDateString = isolationPeriodEndDate.format(DateFormatValidator.formatter);

        val isolationTokenUpdateResponse = authedClient(submitIsolationTokenUpdateRequest(
            TokenUpdateRequest(ipcToken.value, riskyEncounterDateString, isolationPeriodEndDateString)
        ))
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow<TokenUpdateResponse>()
        assertThat(isolationTokenUpdateResponse.websiteUrlWithQuery).endsWith(ipcToken.value)
    }

    fun updateIsolationTokenInvalid(ipcToken: IpcToken, riskyEncounterDateString: String, isolationPeriodEndDateString: String) =
        authedClient(submitIsolationTokenUpdateRequest(TokenUpdateRequest(ipcToken.value, riskyEncounterDateString, isolationPeriodEndDateString)))
            .requireStatusCode(Status.BAD_REQUEST)
            .requireSignatureHeaders()
            .toString()

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
        authedClient(Request(POST, envConfig.diagnosisKeysSubmissionEndpoint)
            .header("Content-Type", APPLICATION_JSON.value)
            .body(Jackson.toJson(payload)))
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .requireNoPayload()
    }

    fun getLatestTwoHourlyTekExport() = diagnosisKeys.getLatestTwoHourlyTekExport()
    fun getDailyTekExport(filename: LocalDate) = diagnosisKeys.getDailyTekExport(filename)
    fun getTwoHourlyTekExport(filename: LocalDateTime) = diagnosisKeys.getTwoHourlyTekExport(filename)

    fun orderTest(version: ApiVersion): VirologyOrderResponse {
        val url = when (version) {
            V1 -> "/home-kit/order"
            V2 -> "/v2/order"
        }

        return authedClient(Request(POST, "${envConfig.virologyKitEndpoint}$url"))
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
    }

    fun pollForTestResult(pollingToken: TestResultPollingToken,
                          version: ApiVersion,
                          country: UserCountry = UserCountry.England): VirologyLookupResult {
        val response = retrieveVirologyResultFor(pollingToken, version, country)

        return when (response.status.code) {
            200 -> when (version) {
                V1 -> VirologyLookupResult.Available(response.requireSignatureHeaders().deserializeOrThrow())
                V2 -> VirologyLookupResult.AvailableV2(response.requireSignatureHeaders().deserializeOrThrow())
            }
            204 -> VirologyLookupResult.Pending()
            404 -> VirologyLookupResult.NotFound()
            else -> throw RuntimeException("Unhandled response")
        }
    }

    fun pollForIncompleteTestResult(orderResponse: VirologyOrderResponse, version: ApiVersion) =
        checkTestResultNotAvailableYet(TestResultPollingToken(orderResponse.testResultPollingToken), version)

    fun exchange(ctaToken: CtaToken,
                 version: ApiVersion,
                 country: UserCountry = UserCountry.England): CtaExchangeResult {
        val (uri, payload) = when (version) {
            V1 -> Pair(
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
                      "country": "${country.value}"
                    }
                """
            )
        }

        val request = Request(POST, uri)
            .header("Content-Type", APPLICATION_JSON.value)
            .header("User-Agent", userAgent())
            .body(payload)

        val response = authedClient(request)

        return when (response.status.code) {
            200 -> when (version) {
                V1 -> CtaExchangeResult.Available(response.deserializeOrThrow())
                V2 -> CtaExchangeResult.AvailableV2(response.deserializeOrThrow())
            }
            404 -> CtaExchangeResult.NotFound()
            else -> throw RuntimeException("Unhandled response")
        }
    }

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
                                          version: ApiVersion,
                                          userCountry: UserCountry = UserCountry.England): Response {
        val (uri, payload) = when (version) {
            V1 -> Pair(
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
                      "country": "${userCountry.value}"
                    }
                """
            )
        }

        return authedClient(Request(POST, uri)
            .header("Content-Type", APPLICATION_JSON.value)
            .header("User-Agent", userAgent())
            .body(payload))
    }

    private fun userAgent() = when (os) {
        iOS -> "p=iOS,o=14.2,v=${appVersion.major}.${appVersion.minor}.${appVersion.patch},b=349"
        Android -> "p=Android,o=29,v=${appVersion.major}.${appVersion.minor}.${appVersion.patch},b=138"
    }
}

class CircuitBreaker(private val authedClient: HttpHandler,
                     private val baseUrl: String,
                     private val payload: String = "") {


    fun request(): TokenResponse =
        authedClient(
            when {
                payload.isNotEmpty() -> Request(POST, "$baseUrl/request")
                    .header("Content-Type", APPLICATION_JSON.value)
                    .body(payload)
                else -> Request(POST, "$baseUrl/request")
            }
        ).requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()

    fun resolve(tokenResponse: TokenResponse): ResolutionResponse =
        authedClient(Request(Method.GET, "$baseUrl/resolution/${tokenResponse.approvalToken}"))
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


class DiagnosisKeysDownload(
    private val unauthedClient: HttpHandler,
    private val envConfig: EnvConfig,
    private val clock: Clock
) {
    fun getLatestTwoHourlyTekExport(): Exposure.TemporaryExposureKeyExport {
        val dateTime = LocalDateTime.ofInstant(Instant.now(clock), ZoneId.of("UTC"))
        return getTwoHourlyTekExport(currentTwoHourlyWindow(dateTime))
    }

    fun getTwoHourlyTekExport(twoHourlyWindow: LocalDateTime): Exposure.TemporaryExposureKeyExport {
        val ofPattern = DateTimeFormatter.ofPattern("yyyyMMddHH")
        val filename = ofPattern.format(twoHourlyWindow) + ".zip"

        val request = Request(Method.GET, "${envConfig.diagnosisKeysDist2hourlyEndpoint}/$filename")

        val response = getCloudfrontContentRetrying(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .requireZipContentType()

        return BatchExport.tekExportFrom(response.body.stream)
    }

    fun getDailyTekExport(date: LocalDate): Exposure.TemporaryExposureKeyExport {
        val fileName = DateTimeFormatter.ofPattern("yyyyMMdd00").format(date) + ".zip"
        val uri = "${envConfig.diagnosisKeysDistributionDailyEndpoint}/$fileName"
        val request = Request(Method.GET, uri)

        val response =
            getCloudfrontContentRetrying(request)
                .requireStatusCode(Status.OK)
                .requireSignatureHeaders()
                .requireZipContentType()

        return BatchExport.tekExportFrom(response.body.stream)
    }

    private fun currentTwoHourlyWindow(dateTime: LocalDateTime) = when {
        dateTime.hour % 2 == 0 -> dateTime.plusHours(2) // use next 2 hour window
        else -> dateTime.plusHours(1) // use current 2 hour window
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
