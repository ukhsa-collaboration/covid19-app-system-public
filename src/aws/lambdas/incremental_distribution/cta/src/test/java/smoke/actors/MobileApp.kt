package smoke.actors

import assertions.CircuitBreakersAssertions.approval
import assertions.CircuitBreakersAssertions.approvalToken
import batchZipCreation.Exposure
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ResilienceFilters
import smoke.actors.ApiVersion.V1
import smoke.actors.ApiVersion.V2
import smoke.data.DiagnosisKeyData.createKeysPayload
import smoke.data.DiagnosisKeyData.createKeysPayloadWithOnsetDays
import smoke.env.EnvConfig
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isNullOrEmpty
import uk.nhs.nhsx.analyticssubmission.analyticsSubmissionAndroidComplete
import uk.nhs.nhsx.analyticssubmission.analyticsSubmissionIosComplete
import uk.nhs.nhsx.circuitbreakers.ResolutionResponse
import uk.nhs.nhsx.circuitbreakers.TokenResponse
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.Json.readJsonOrThrow
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.headers.MobileOS.Android
import uk.nhs.nhsx.core.headers.MobileOS.Unknown
import uk.nhs.nhsx.core.headers.MobileOS.iOS
import uk.nhs.nhsx.crashreports.CrashReportRequest
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse.Disabled
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse.OK
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import uk.nhs.nhsx.testhelper.BatchExport
import uk.nhs.nhsx.testhelper.data.TestData.EXPOSURE_NOTIFICATION_CIRCUIT_BREAKER_PAYLOAD
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult
import uk.nhs.nhsx.virology.order.VirologyOrderResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MobileApp(
    private val unAuthedClient: HttpHandler,
    private val envConfig: EnvConfig,
    private val os: MobileOS = iOS,
    private val appVersion: MobileAppVersion.Version = MobileAppVersion.Version(4, 4),
    private val model: MobileDeviceModel? = null,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    private val authedClient = SetAuthHeader(envConfig.auth_headers.mobile)
        .then(ResilienceFilters.RetryFailures(isError = { !it.status.successful }))
        .then(unAuthedClient)
    val exposureCircuitBreaker = CircuitBreaker(
        authedClient,
        envConfig.exposure_notification_circuit_breaker_endpoint,
        EXPOSURE_NOTIFICATION_CIRCUIT_BREAKER_PAYLOAD
    )
    val venueCircuitBreaker = CircuitBreaker(authedClient, envConfig.risky_venues_circuit_breaker_endpoint)

    private val diagnosisKeys = DiagnosisKeysDownload(unAuthedClient, envConfig, clock)
    private var orderedTest: VirologyOrderResponse? = null

    fun pollRiskyPostcodes(version: ApiVersion): Map<String, Any> =
        readJsonOrThrow(
            when (version) {
                V1 -> getStaticContent(envConfig.post_districts_distribution_endpoint)
                V2 -> getStaticContent(envConfig.post_districts_distribution_endpoint + "-v2")
            }
        )

    fun pollRiskyVenues() = deserializeWithOptional(getStaticContent(envConfig.risky_venues_distribution_endpoint))!!

    fun pollAvailability() = when (os) {
        iOS, Unknown -> getStaticContent(envConfig.availability_ios_distribution_endpoint)
        Android -> getStaticContent(envConfig.availability_android_distribution_endpoint)
    }

    fun pollExposureConfig() = getStaticContent(envConfig.exposure_configuration_distribution_endpoint)
    fun pollSelfIsolation() = getStaticContent(envConfig.self_isolation_distribution_endpoint)
    fun pollSymptomaticQuestionnaire() = getStaticContent(envConfig.symptomatic_questionnaire_distribution_endpoint)
    fun pollRiskyVenueConfiguration() = getStaticContent(envConfig.risky_venue_configuration_distribution_endpoint)

    fun submitAnalyticsKeys(startDate: Instant, endDate: Instant): Status {
        val json = when (os) {
            iOS -> analyticsSubmissionIosComplete(
                deviceModel = model?.value ?: "iPhone-smoke-test",
                localAuthority = "E07000240",
                postalDistrict = "AL1",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                useCounter = true
            )
            Android -> analyticsSubmissionAndroidComplete(
                deviceModel = model?.value ?: "HUAWEI-smoke-test",
                localAuthority = "E07000240",
                postalDistrict = "AL1",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                useCounter = true
            )
            Unknown -> throw RuntimeException("Unknown device os. Set one for ${this::class.java.simpleName}?")
        }
        return authedClient(
            Request(POST, envConfig.analytics_submission_endpoint)
                .header("Content-Type", ContentType("text/json").value)
                .body(json)
        ).status
    }

    fun orderTest(): VirologyOrderResponse {
        orderedTest = authedClient(Request(POST, "${envConfig.virology_kit_endpoint}/home-kit/order"))
            .requireStatusCode(OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
        return orderedTest!!
    }

    fun registerTest(): VirologyOrderResponse {
        orderedTest = authedClient(Request(POST, "${envConfig.virology_kit_endpoint}/home-kit/register"))
            .requireStatusCode(OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()
        return orderedTest!!
    }

    fun submitAnalyticEvents(json: String): Response = authedClient(
        Request(POST, envConfig.analytics_events_submission_endpoint)
            .header("Content-Type", ContentType("text/json").value)
            .body(json)
    )

    fun emptySubmission(): Response {
        return authedClient(Request(POST, envConfig.empty_submission_endpoint))
    }

    fun emptySubmissionV2(): Response {
        return unAuthedClient(Request(GET, envConfig.empty_submission_v2_endpoint))
    }

    fun submitKeys(
        diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
        encodedSubmissionKeys: List<String>
    ): ClientTemporaryExposureKeysPayload {
        val payload = createKeysPayload(diagnosisKeySubmissionToken, encodedSubmissionKeys, clock)
        sendTempExposureKeys(payload)
        return payload
    }

    fun submitKeysWithOnsetDays(
        diagnosisKeySubmissionToken: DiagnosisKeySubmissionToken,
        encodedSubmissionKeys: List<String>
    ): ClientTemporaryExposureKeysPayload {
        val payload = createKeysPayloadWithOnsetDays(diagnosisKeySubmissionToken, encodedSubmissionKeys, clock)
        sendTempExposureKeys(payload)
        return payload
    }

    fun createIsolationToken(country: Country) = authedClient(
        Request(POST, envConfig.isolation_payment_create_endpoint)
            .header("Content-Type", APPLICATION_JSON.value)
            .body(Json.toJson(TokenGenerationRequest(country)))
    )
        .requireStatusCode(Status.CREATED)
        .requireSignatureHeaders()
        .deserializeOrThrow<OK>()

    fun createNonWhiteListedIsolationToken(country: Country) = authedClient(
        Request(POST, envConfig.isolation_payment_create_endpoint)
            .header("Content-Type", APPLICATION_JSON.value)
            .body(Json.toJson(TokenGenerationRequest(country)))
    )
        .requireStatusCode(Status.CREATED)
        .requireSignatureHeaders()
        .deserializeOrThrow<Disabled>()

    fun updateIsolationToken(ipcToken: IpcTokenId, riskyEncounterDate: Instant, isolationPeriodEndDate: Instant) {
        val isolationTokenUpdateResponse = authedClient(
            submitIsolationTokenUpdateRequest(
                TokenUpdateRequest(ipcToken, riskyEncounterDate, isolationPeriodEndDate)
            )
        )
            .requireStatusCode(OK)
            .requireSignatureHeaders()
            .deserializeOrThrow<TokenUpdateResponse>()
        expectThat(isolationTokenUpdateResponse.websiteUrlWithQuery).endsWith(ipcToken.value)
    }

    private fun getStaticContent(uri: String) = unAuthedClient(Request(GET, uri))
        .requireStatusCode(OK)
        .requireSignatureHeaders()
        .requireJsonContentType()
        .bodyString()

    private fun submitIsolationTokenUpdateRequest(updateRequest: TokenUpdateRequest): Request =
        Request(POST, envConfig.isolation_payment_update_endpoint)
            .header("Content-Type", "application/json")
            .body(Json.toJson(updateRequest))


    private fun sendTempExposureKeys(payload: ClientTemporaryExposureKeysPayload) {
        authedClient(
            Request(POST, envConfig.diagnosis_keys_submission_endpoint)
                .header("Content-Type", APPLICATION_JSON.value)
                .body(Json.toJson(payload))
                .header("UserAgent", "p=Android,o=29,v=4.7,b=168")
                .header("User-Agent", "p=Android,o=29,v=4.7,b=168")
        )
            .requireStatusCode(OK)
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

        val response = authedClient(Request(POST, "${envConfig.virology_kit_endpoint}$url"))
            .requireStatusCode(OK)
            .requireSignatureHeaders()

        return when (version) {
            V1 -> response.deserializeOrThrow()
            V2 -> response.deserializeOrThrow()
        }
    }

    fun pollForTestResult(
        pollingToken: TestResultPollingToken,
        version: ApiVersion,
        country: Country = England
    ): VirologyLookupResult {
        val response = retrieveVirologyResultFor(pollingToken, version, country)

        return when (response.status.code) {
            200 -> when (version) {
                V1 -> VirologyLookupResult.Available(response.requireSignatureHeaders().deserializeOrThrow())
                V2 -> VirologyLookupResult.AvailableV2(
                    response.requireSignatureHeaders().deserializeWithNullCreatorsOrThrow()
                )
            }
            204 -> VirologyLookupResult.Pending()
            404 -> VirologyLookupResult.NotFound()
            else -> throw RuntimeException("Unhandled response")
        }
    }

    fun pollForIncompleteTestResult(orderResponse: VirologyOrderResponse, version: ApiVersion) =
        checkTestResultNotAvailableYet(orderResponse.testResultPollingToken, version)

    fun exchange(
        ctaToken: CtaToken,
        version: ApiVersion,
        country: Country = England
    ): CtaExchangeResult {
        val (uri, payload) = when (version) {
            V1 -> Pair(
                "${envConfig.virology_kit_endpoint}/cta-exchange",
                """
                    {
                      "ctaToken": "${ctaToken.value}"
                    }
                """
            )
            V2 -> Pair(
                "${envConfig.virology_kit_endpoint}/v2/cta-exchange",
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
                V2 -> CtaExchangeResult.AvailableV2(response.deserializeWithNullCreatorsOrThrow())
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

    private fun retrieveVirologyResultFor(
        pollingToken: TestResultPollingToken,
        version: ApiVersion,
        userCountry: Country = England
    ): Response {
        val (uri, payload) = when (version) {
            V1 -> Pair(
                "${envConfig.virology_kit_endpoint}/results",
                """
                    {
                      "testResultPollingToken": "${pollingToken.value}"
                    }
                """
            )
            V2 -> Pair(
                "${envConfig.virology_kit_endpoint}/v2/results",
                """
                    {
                      "testResultPollingToken": "${pollingToken.value}", 
                      "country": "${userCountry.value}"
                    }
                """
            )
        }

        return authedClient(
            Request(POST, uri)
                .header("Content-Type", APPLICATION_JSON.value)
                .header("User-Agent", userAgent())
                .body(payload)
        )
    }

    private fun userAgent() = when (os) {
        iOS, Unknown -> "p=iOS,o=14.2,v=${appVersion.major}.${appVersion.minor}.${appVersion.patch},b=349"
        Android -> "p=Android,o=29,v=${appVersion.major}.${appVersion.minor}.${appVersion.patch},b=138"
    }

    fun submitCrashReport(crashReportRequest: CrashReportRequest) {
        authedClient(
            Request(POST, envConfig.crash_reports_submission_endpoint)
                .header("Content-Type", "application/json")
                .body(Json.toJson(crashReportRequest))
        ).requireStatusCode(OK).requireNoPayload()
    }
}

class CircuitBreaker(
    private val authedClient: HttpHandler,
    private val baseUrl: String,
    private val payload: String = ""
) {
    fun request(): TokenResponse = authedClient(
        when {
            payload.isNotEmpty() -> Request(POST, "$baseUrl/request")
                .header("Content-Type", APPLICATION_JSON.value)
                .body(payload)
            else -> Request(POST, "$baseUrl/request")
        }
    ).requireStatusCode(OK)
        .requireSignatureHeaders()
        .deserializeOrThrow()

    private fun resolve(tokenResponse: TokenResponse): ResolutionResponse =
        authedClient(Request(GET, "$baseUrl/resolution/${tokenResponse.approvalToken}"))
            .requireStatusCode(OK)
            .requireSignatureHeaders()
            .deserializeOrThrow()

    fun requestAndApproveCircuitBreak() {
        val tokenResponse: TokenResponse = request()

        expectThat(tokenResponse) {
            approval.isEqualTo("yes")
            approvalToken.not().isNullOrEmpty()
            get(::resolve).get(ResolutionResponse::approval).isEqualTo("yes")
        }
    }
}

private fun deserializeWithOptional(staticContentRiskyVenues: String) =
    Json.readStrictOrNull<HighRiskVenues>(staticContentRiskyVenues)

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

        val request = Request(GET, "${envConfig.diagnosis_keys_distribution_2hourly_endpoint}/$filename")

        val response = getCloudfrontContentRetrying(request)
            .requireStatusCode(OK)
            .requireSignatureHeaders()
            .requireZipContentType()

        return BatchExport.tekExportFrom(response.body.stream)
    }

    fun getDailyTekExport(date: LocalDate): Exposure.TemporaryExposureKeyExport {
        val fileName = DateTimeFormatter.ofPattern("yyyyMMdd00").format(date) + ".zip"
        val uri = "${envConfig.diagnosis_keys_distribution_daily_endpoint}/$fileName"
        val request = Request(GET, uri)

        val response = getCloudfrontContentRetrying(request)
            .requireStatusCode(OK)
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
            if (response.status == OK) {
                return response
            }
            numberOfTries++
            val sleepDuration = Duration.ofSeconds(numberOfTries).plus(retryWaitDuration)

            if (numberOfTries <= maxRetries) Thread.sleep(sleepDuration.toMillis())
        } while (numberOfTries <= maxRetries)

        throw IllegalStateException("Tried to fetch cloudfront content but failed after $maxRetries attempts")
    }
}
