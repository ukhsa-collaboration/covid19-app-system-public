package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.natpryce.snodge.json.defaultJsonMutagens
import com.natpryce.snodge.json.forStrings
import com.natpryce.snodge.mutants
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.MobileAnalyticsSubmission
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.BAD_REQUEST_400
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.FORBIDDEN_403
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.INTERNAL_SERVER_ERROR_500
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.METHOD_NOT_ALLOWED_405
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.NOT_FOUND_404
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.OK_200
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_EMPTY_PAIR
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_INVALID_PAIR
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_ANDROID
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_IOS
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_PAYLOAD_ANDROID
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_PAYLOAD_ANDROID_WITH_LOCAL_AUTHORITY
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_PAYLOAD_IOS
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_PAYLOAD_IOS_NEW_METRICS
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_PAYLOAD_IOS_WITH_LOCAL_AUTHORITY
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_UNKNOWN_POSTCODE_PAYLOAD_ANDROID
import uk.nhs.nhsx.testhelper.data.TestData.STORED_ANALYTICS_UNKNOWN_POSTCODE_PAYLOAD_IOS
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasBody
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
import uk.nhs.nhsx.testhelper.mocks.FakeS3Storage
import java.util.function.Consumer
import kotlin.random.Random

class AnalyticsSubmissionHandlerTest {
    private val objectKey = ObjectKey.of("some-object-key")
    private val s3Storage = FakeS3Storage()
    private val kinesisFirehose = mockk<AmazonKinesisFirehose>()
    private val objectKeyNameProvider = mockk<ObjectKeyNameProvider>()
    private val config = AnalyticsConfig(
        "firehoseStreamName",
        true,
        false,
        BUCKET_NAME
    )
    private val events = RecordingEvents()
    private val handler = AnalyticsSubmissionHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ),
        { true },
        { true },
        s3Storage,
        kinesisFirehose,
        objectKeyNameProvider,
        config,
        events
    )

    @BeforeEach
    fun setup() {
        every { objectKeyNameProvider.generateObjectKeyName() } returns objectKey
    }

    @Test
    fun acceptsiOSPayloadAndReturns200() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_PAYLOAD_IOS))

        events.containsExactly(MobileAnalyticsSubmission::class)
    }

    @Test
    fun acceptsiOSPayloadMergesPostDistrictsAndReturns200() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "AB13"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_IOS))
        events.containsExactly(MobileAnalyticsSubmission::class)
    }

    @Test
    fun acceptsiOSPayloadWithNewMetricFieldsAndReturns200() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFromNewMetrics("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_PAYLOAD_IOS_NEW_METRICS))
        events.containsExactly(MobileAnalyticsSubmission::class)
    }

    @Test
    fun iosPayloadWithInvalidPairOfValidPostCodeAndValidLocalAuthority() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(
                iOSPayloadFromWithMetrics(
                    "2020-07-27T23:00:00Z",
                    "2020-07-28T22:59:00Z",
                    "YO62",
                    "",
                    "E07000152"
                )
            )
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_INVALID_PAIR))
    }

    @Test
    fun iosPayloadWithInvalidLocalAuthority() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFromWithMetrics("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "YO62", "", "Houston"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_INVALID_PAIR))
    }

    @Test
    fun iosPayloadWithEmptyPostCodeAndLocalAuthority() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFromWithMetrics("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "", "", ""))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_EMPTY_PAIR))
    }

    @Test
    fun iosPayloadWithPostcodeNotFoundInMappingSavesPostDistrictAsUnknown() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "F4KEP0STC0DE"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_UNKNOWN_POSTCODE_PAYLOAD_IOS))
        events.containsExactly(MobileAnalyticsSubmission::class, InfoEvent::class)
    }

    @Test
    fun androidPayloadWithPostcodeNotFoundInMappingSavesPostDistrictAsUnknown() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "F4KEP0STC0DE"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_UNKNOWN_POSTCODE_PAYLOAD_ANDROID))
        events.containsExactly(MobileAnalyticsSubmission::class, InfoEvent::class)
    }

    @Test
    fun acceptsAndroidPayloadMergesPostDistrictsAndReturns200() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "AB13"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_ANDROID))
        events.containsExactly(MobileAnalyticsSubmission::class)
    }

    @Test
    fun acceptsAndroidPayloadAndReturns200() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_PAYLOAD_ANDROID))
        events.containsExactly(MobileAnalyticsSubmission::class)
    }

    @Test
    fun notFoundWhenPathIsWrong() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withPath("dodgy")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(NOT_FOUND_404))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(0))
    }

    @Test
    fun notAllowedWhenMethodIsWrong() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("something")
            .withPath("/submission/mobile-analytics")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(METHOD_NOT_ALLOWED_405))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(0))
    }

    @Test
    fun badRequestWhenEmptyBody() {
        val responseEvent = responseFor("")
        assertThat(responseEvent, hasStatus(BAD_REQUEST_400))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(0))
        events.containsExactly(MobileAnalyticsSubmission::class, UnprocessableJson::class)
    }

    @Test
    fun badRequestWhenMalformedJson() {
        val responseEvent = responseFor("{")
        assertThat(responseEvent, hasStatus(BAD_REQUEST_400))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(0))
        events.containsExactly(MobileAnalyticsSubmission::class, UnprocessableJson::class)
    }

    @Test
    fun badRequestWhenEmptyJsonObject() {
        val responseEvent = responseFor("{}")
        assertThat(responseEvent, hasStatus(BAD_REQUEST_400))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(0))
        events.containsExactly(MobileAnalyticsSubmission::class, UnprocessableJson::class)
    }

    @Disabled("Mutated postcode won't be in mapping causing a 500 error")
    @Test
    fun randomPayloadValues() {
        val originalJson = iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z")
        Random.mutants(defaultJsonMutagens().forStrings(), 100, originalJson)
            .forEach(Consumer { json: String ->
                if (json != originalJson) {
                    val response = responseFor(json)
                    assertThat(
                        response, not(
                            anyOf(
                                hasStatus(INTERNAL_SERVER_ERROR_500),
                                hasStatus(FORBIDDEN_403)
                            )
                        )
                    )
                    assertThat(response, hasBody(equalTo(null)))
                }
            })
    }

    @Test
    fun badRequestWhenStartDateIsInInvalidFormat() {
        val responseEvent = responseFor(
            iOSPayloadFrom("2020-06-2001:00:00Z", "2020-06-20T22:00:00Z")
        )
        assertThat(responseEvent, hasStatus(BAD_REQUEST_400))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(0))
        events.containsExactly(MobileAnalyticsSubmission::class, UnprocessableJson::class)
    }

    @Test
    fun badRequestWhenEndDateBeforeStartDate() {
        val responseEvent = responseFor(
            iOSPayloadFrom("2020-06-20T22:00:00Z", "2020-06-20T22:00:00")
        )
        assertThat(responseEvent, hasStatus(BAD_REQUEST_400))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(0))
        events.containsExactly(MobileAnalyticsSubmission::class, UnprocessableJson::class)
    }

    private fun responseFor(requestPayload: String): APIGatewayProxyResponseEvent {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withPath("/submission/mobile-analytics")
            .withBody(requestPayload)
            .build()
        return handler.handleRequest(requestEvent, ContextBuilder.aContext())
    }

    @Test
    fun acceptsiOSPayloadWithLocalAuthorityAndReturns200() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFromWithLocalAuthority("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_PAYLOAD_IOS_WITH_LOCAL_AUTHORITY))
        events.containsExactly(MobileAnalyticsSubmission::class)
    }

    @Test
    fun acceptsAndroidPayloadWithLocalAuthorityAndReturns200() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "SY5", "E06000051"))
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(STORED_ANALYTICS_PAYLOAD_ANDROID_WITH_LOCAL_AUTHORITY))
        events.containsExactly(MobileAnalyticsSubmission::class)
    }

    companion object {
        private val BUCKET_NAME = BucketName.of("some-bucket-name")
        fun iOSPayloadFromNewMetrics(startDate: String, endDate: String): String {
            val metrics = """
                    ,"receivedVoidTestResultEnteredManually" : 1,    
                    "receivedPositiveTestResultEnteredManually" : 1,    
                    "receivedNegativeTestResultEnteredManually" : 0,    
                    "receivedVoidTestResultViaPolling" : 0,    
                    "receivedPositiveTestResultViaPolling" : 0,    
                    "receivedNegativeTestResultViaPolling" : 1,    
                    "hasSelfDiagnosedBackgroundTick" : 4,    
                    "hasTestedPositiveBackgroundTick" : 5,    
                    "isIsolatingForSelfDiagnosedBackgroundTick" : 6,    
                    "isIsolatingForTestedPositiveBackgroundTick" : 3,    
                    "isIsolatingForHadRiskyContactBackgroundTick" : 13,    
                    "receivedRiskyContactNotification" : 1,    
                    "startedIsolation" : 1,    
                    "receivedPositiveTestResultWhenIsolatingDueToRiskyContact" : 1,
                    "receivedActiveIpcToken": 1,
                    "haveActiveIpcTokenBackgroundTick": 1,
                    "selectedIsolationPaymentsButton": 1,
                    "launchedIsolationPaymentsApplication": 1,
                    "receivedPositiveLFDTestResultViaPolling":  1,
                    "receivedNegativeLFDTestResultViaPolling":  1,
                    "receivedVoidLFDTestResultViaPolling":  1,
                    "receivedPositiveLFDTestResultEnteredManually": 1,
                    "receivedNegativeLFDTestResultEnteredManually": 1,
                    "receivedVoidLFDTestResultEnteredManually": 1,
                    "hasTestedLFDPositiveBackgroundTick": 1,
                    "isIsolatingForTestedLFDPositiveBackgroundTick": 1,
                    "totalExposureWindowsNotConsideredRisky": 1,
                    "totalExposureWindowsConsideredRisky": 1,
                    "acknowledgedStartOfIsolationDueToRiskyContact":1,
                    "hasRiskyContactNotificationsEnabledBackgroundTick":1,
                    "totalRiskyContactReminderNotifications":1,
                    "receivedUnconfirmedPositiveTestResult":1,
                    "isIsolatingForUnconfirmedTestBackgroundTick":1,
                    "launchedTestOrdering":1,
                    "didHaveSymptomsBeforeReceivedTestResult":1,
                    "didRememberOnsetSymptomsDateBeforeReceivedTestResult":1,
                    "didAskForSymptomsOnPositiveTestEntry":1,
                    "declaredNegativeResultFromDCT":1""".trimIndent()
            return iOSPayloadFromWithMetrics(startDate, endDate, "AB10", metrics)
        }

        fun iOSPayloadFromWithMetrics(
            startDate: String,
            endDate: String,
            postDistrict: String,
            metrics: String
        ): String {
            return """
                {
                  "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "$postDistrict"
                  },
                  "analyticsWindow": {
                    "endDate": "$endDate",
                    "startDate": "$startDate"
                  },
                  "metrics": {
                    "cumulativeDownloadBytes": 140000000,
                    "cumulativeUploadBytes": 140000000,
                    "cumulativeCellularDownloadBytes": 80000000,
                    "cumulativeCellularUploadBytes": 70000000,
                    "cumulativeWifiDownloadBytes": 60000000,
                    "cumulativeWifiUploadBytes": 50000000,
                    "checkedIn": 1,
                    "canceledCheckIn": 1,
                    "receivedVoidTestResult": 1,
                    "isIsolatingBackgroundTick": 1,
                    "hasHadRiskyContactBackgroundTick": 1,
                    "receivedPositiveTestResult": 1,
                    "receivedNegativeTestResult": 1,
                    "hasSelfDiagnosedPositiveBackgroundTick": 1,
                    "completedQuestionnaireAndStartedIsolation": 1,
                    "encounterDetectionPausedBackgroundTick": 1,
                    "completedQuestionnaireButDidNotStartIsolation": 1,
                    "totalBackgroundTasks": 1,
                    "runningNormallyBackgroundTick": 1,
                    "completedOnboarding": 1
                    $metrics
                  },
                  "includesMultipleApplicationVersions": false
                }
            """.trimIndent()
        }

        private fun iOSPayloadFromWithMetrics(
            startDate: String,
            endDate: String,
            postDistrict: String,
            metrics: String,
            localAuthority: String
        ): String {
            return """
                {
                  "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "$postDistrict",
                    "localAuthority": "$localAuthority"
                  },
                  "analyticsWindow": {
                    "endDate": "$endDate",
                    "startDate": "$startDate"
                  },
                  "metrics": {
                    "cumulativeDownloadBytes": 140000000,
                    "cumulativeUploadBytes": 140000000,
                    "cumulativeCellularDownloadBytes": 80000000,
                    "cumulativeCellularUploadBytes": 70000000,
                    "cumulativeWifiDownloadBytes": 60000000,
                    "cumulativeWifiUploadBytes": 50000000,
                    "checkedIn": 1,
                    "canceledCheckIn": 1,
                    "receivedVoidTestResult": 1,
                    "isIsolatingBackgroundTick": 1,
                    "hasHadRiskyContactBackgroundTick": 1,
                    "receivedPositiveTestResult": 1,
                    "receivedNegativeTestResult": 1,
                    "hasSelfDiagnosedPositiveBackgroundTick": 1,
                    "completedQuestionnaireAndStartedIsolation": 1,
                    "encounterDetectionPausedBackgroundTick": 1,
                    "completedQuestionnaireButDidNotStartIsolation": 1,
                    "totalBackgroundTasks": 1,
                    "runningNormallyBackgroundTick": 1,
                    "completedOnboarding": 1
                    $metrics
                  },
                  "includesMultipleApplicationVersions": false
                }
            """.trimIndent()
        }

        private fun iOSPayloadFrom(startDate: String, endDate: String): String {
            return iOSPayloadFrom(startDate, endDate, "AB10")
        }

        fun iOSPayloadFrom(startDate: String, endDate: String, postDistrict: String): String {
            return iOSPayloadFromWithMetrics(startDate, endDate, postDistrict, "")
        }

        fun iOSPayloadFromWithLocalAuthority(startDate: String, endDate: String): String {
            return iOSPayloadFromWithMetrics(startDate, endDate, "SY5", "", "E06000051")
        }


        private fun androidPayloadFrom(startDate: String, endDate: String, postDistrict: String = "AB10"): String {
            return """
                {
                   "metadata":{
                      "operatingSystemVersion":"29",
                      "latestApplicationVersion":"3.0",
                      "deviceModel":"HUAWEI LDN-L21",
                      "postalDistrict":"$postDistrict"
                   },
                   "analyticsWindow":{
                      "endDate":"$endDate",
                      "startDate":"$startDate"
                   },
                   "metrics":{
                      "cumulativeDownloadBytes":null,
                      "cumulativeUploadBytes":null,
                      "checkedIn":1,
                      "canceledCheckIn":1,
                      "receivedVoidTestResult":1,
                      "isIsolatingBackgroundTick":1,
                      "hasHadRiskyContactBackgroundTick":1,
                      "receivedPositiveTestResult":1,
                      "receivedNegativeTestResult":1,
                      "hasSelfDiagnosedPositiveBackgroundTick":1,
                      "completedQuestionnaireAndStartedIsolation":1,
                      "encounterDetectionPausedBackgroundTick":1,
                      "completedQuestionnaireButDidNotStartIsolation":1,
                      "totalBackgroundTasks":1,
                      "runningNormallyBackgroundTick":1,
                      "completedOnboarding":1
                   },
                   "includesMultipleApplicationVersions":false
                }""".trimIndent()
        }

        private fun androidPayloadFrom(
            startDate: String,
            endDate: String,
            postDistrict: String = "AB10",
            localAuthority: String
        ): String {
            return """
                {
                   "metadata":{
                      "operatingSystemVersion":"29",
                      "latestApplicationVersion":"3.0",
                      "deviceModel":"HUAWEI LDN-L21",
                      "postalDistrict":"$postDistrict",
                      "localAuthority":"$localAuthority"
                   },
                   "analyticsWindow":{
                      "endDate":"$endDate",
                      "startDate":"$startDate"
                   },
                   "metrics":{
                      "cumulativeDownloadBytes":null,
                      "cumulativeUploadBytes":null,
                      "checkedIn":1,
                      "canceledCheckIn":1,
                      "receivedVoidTestResult":1,
                      "isIsolatingBackgroundTick":1,
                      "hasHadRiskyContactBackgroundTick":1,
                      "receivedPositiveTestResult":1,
                      "receivedNegativeTestResult":1,
                      "hasSelfDiagnosedPositiveBackgroundTick":1,
                      "completedQuestionnaireAndStartedIsolation":1,
                      "encounterDetectionPausedBackgroundTick":1,
                      "completedQuestionnaireButDidNotStartIsolation":1,
                      "totalBackgroundTasks":1,
                      "runningNormallyBackgroundTick":1,
                      "completedOnboarding":1
                   },
                   "includesMultipleApplicationVersions":false
                }""".trimIndent()
        }
    }
}
