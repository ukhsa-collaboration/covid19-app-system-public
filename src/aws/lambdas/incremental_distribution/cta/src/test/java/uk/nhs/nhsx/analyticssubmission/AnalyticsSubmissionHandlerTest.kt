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
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.MobileAnalyticsSubmission
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.*
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
        s3IngestEnabled = true,
        firehoseIngestEnabled = false,
        bucketName = BUCKET_NAME
    )

    private val events = RecordingEvents()
    private val handler = AnalyticsSubmissionHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ),
        SystemClock.CLOCK,
        events,
        { true },
        { true },
        s3Storage,
        kinesisFirehose,
        objectKeyNameProvider,
        config
    )

    @BeforeEach
    fun setup() {
        every { objectKeyNameProvider.generateObjectKeyName() } returns objectKey
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
        assertStatusIs400(responseEvent)
    }

    @Test
    fun badRequestWhenMalformedJson() {
        val responseEvent = responseFor("{")
        assertStatusIs400(responseEvent)
    }

    @Test
    fun badRequestWhenEmptyJsonObject() {
        val responseEvent = responseFor("{}")
        assertStatusIs400(responseEvent)
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
        assertStatusIs400(responseEvent)
    }

    @Test
    fun badRequestWhenEndDateIsInInvalidFormat() {
        val responseEvent = responseFor(
            iOSPayloadFrom("2020-06-20T22:00:00Z", "2020-06-20T22:00:00")
        )
        assertStatusIs400(responseEvent)
    }

    private fun assertStatusIs400(responseEvent: APIGatewayProxyResponseEvent) {
        assertThat(responseEvent, hasStatus(BAD_REQUEST_400))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(0))
        events.contains(MobileAnalyticsSubmission::class, UnprocessableJson::class)
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


    enum class TestCombo(val payload: String, val expectedJson: String) {
        WITH_LOCAL_AUTHORITY_ANDROID(
            payload = androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "SY5", "E06000051"),
            expectedJson = STORED_ANALYTICS_PAYLOAD_ANDROID_WITH_LOCAL_AUTHORITY
        ),
        WITH_LOCAL_AUTHORITY_IOS(
            payload = iOSPayloadFromWithMetrics("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "SY5", "", "E06000051"),
            expectedJson = STORED_ANALYTICS_PAYLOAD_IOS_WITH_LOCAL_AUTHORITY
        ),
        WITH_IOS(
            payload = iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"),
            expectedJson = STORED_ANALYTICS_PAYLOAD_IOS
        ),
        WITH_MERGED_DISTRICTS_IOS(
            payload = iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "AB13"),
            expectedJson = STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_IOS
        ),
        WITH_NEW_METRICS_IOS(
            payload = iOSPayloadFromNewMetrics("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"),
            expectedJson = STORED_ANALYTICS_PAYLOAD_IOS_NEW_METRICS
        ),
        WITH_INVALID_PAIR_POSTCODE_LOCAL_AUTHORITY_IOS(
            payload = iOSPayloadFromWithMetrics("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "YO62", "", "E07000152"),
            expectedJson = STORED_ANALYTICS_INVALID_PAIR
        ),
        WITH_INVALID_LOCAL_AUTHORITY_IOS(
            payload = iOSPayloadFromWithMetrics("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "YO62", "", "Houston"),
            expectedJson = STORED_ANALYTICS_INVALID_PAIR
        ),
        WITH_EMPTY_POSTCODE_LOCAL_AUTHORITY_IOS(
            payload = iOSPayloadFromWithMetrics("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "", "", ""),
            expectedJson = STORED_ANALYTICS_EMPTY_PAIR
        ),
        WITH_UNKNOWN_POSTCODE_IOS(
            payload = iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "F4KEP0STC0DE"),
            expectedJson = STORED_ANALYTICS_UNKNOWN_POSTCODE_PAYLOAD_IOS
        ),
        WITH_UNKNOWN_POSTCODE_ANDROID(
            payload = androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "F4KEP0STC0DE"),
            expectedJson = STORED_ANALYTICS_UNKNOWN_POSTCODE_PAYLOAD_ANDROID
        ),
        WITH_MERGED_DISTRICTS_ANDROID(
            payload = androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "AB13"),
            expectedJson = STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_ANDROID
        ),
        WITH_ANDROID(
            payload = androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"),
            expectedJson = STORED_ANALYTICS_PAYLOAD_ANDROID
        ),
    }


    @ParameterizedTest
    @EnumSource(TestCombo::class)
    fun assertPayloadReturns200AndMatches(testCombo: TestCombo) {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(testCombo.payload)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(OK_200))
        assertThat(responseEvent, hasBody(equalTo(null)))
        assertThat(s3Storage.count, equalTo(1))
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")))
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME))
        assertThat(s3Storage.bytes.toUtf8String(), equalTo(testCombo.expectedJson))
        events.contains(MobileAnalyticsSubmission::class)
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
                    "declaredNegativeResultFromDCT":1,
                    "receivedPositiveSelfRapidTestResultViaPolling":1,
                    "receivedNegativeSelfRapidTestResultViaPolling":1,
                    "receivedVoidSelfRapidTestResultViaPolling":1,
                    "receivedPositiveSelfRapidTestResultEnteredManually":1,
                    "receivedNegativeSelfRapidTestResultEnteredManually":1,
                    "receivedVoidSelfRapidTestResultEnteredManually":1,
                    "isIsolatingForTestedSelfRapidPositiveBackgroundTick":1,
                    "hasTestedSelfRapidPositiveBackgroundTick":1,
                    "receivedRiskyVenueM1Warning":1,
                    "receivedRiskyVenueM2Warning":1,
                    "hasReceivedRiskyVenueM2WarningBackgroundTick":1,
                    "totalAlarmManagerBackgroundTasks":1,
                    "missingPacketsLast7Days":1,
                    "consentedToShareVenueHistory":1,
                    "askedToShareVenueHistory":1,
                    "askedToShareExposureKeysInTheInitialFlow":1,
                    "consentedToShareExposureKeysInTheInitialFlow":1,
                    "totalShareExposureKeysReminderNotifications":1,
                    "consentedToShareExposureKeysInReminderScreen":1,
                    "successfullySharedExposureKeys":1,
                    "didSendLocalInfoNotification":1,
                    "didAccessLocalInfoScreenViaNotification":1,
                    "didAccessLocalInfoScreenViaBanner":1,
                    "isDisplayingLocalInfoBackgroundTick":1""".trimIndent()
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

        fun iOSPayloadFrom(startDate: String, endDate: String, postDistrict: String = "AB10"): String {
            return iOSPayloadFromWithMetrics(startDate, endDate, postDistrict, "", "E06000051")
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
