@file:Suppress("SameParameterValue")

package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isNull
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.MobileAnalyticsSubmission
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId
import java.nio.charset.Charset

class AnalyticsSubmissionHandlerTest {

    private val events = RecordingEvents()

    private val objectKey = ObjectKey.of("some-object-key")

    private val kinesisFirehose = mockk<AmazonKinesisFirehose>()
    private val objectKeyNameProvider = mockk<ObjectKeyNameProvider>()

    private val config = AnalyticsConfig("firehoseStreamName", firehoseIngestEnabled = true)
    private val handler = AnalyticsSubmissionHandler(
        environment = TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ),
        clock = SystemClock.CLOCK,
        events = events,
        healthAuthenticator = { true },
        mobileAuthenticator = { true },
        kinesisFirehose = kinesisFirehose,
        objectKeyNameProvider = objectKeyNameProvider,
        analyticsConfig = config
    )

    @BeforeEach
    fun setup() {
        every { objectKeyNameProvider.generateObjectKeyName() } returns objectKey
    }

    @Test
    fun `not found when path is wrong`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withPath("dodgy")
            .withJson(
                analyticsPayloadFrom(
                    startDate = "2020-07-27T23:00:00Z",
                    endDate = "2020-07-28T22:59:00Z",
                    postDistrict = "AB10",
                    localAuthority = "E06000051"
                )
            )

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        expect {
            that(responseEvent) {
                status.isSameAs(NOT_FOUND)
                body.isNull()
            }
        }
    }

    @Test
    fun `not allowed when method is wrong`() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("something")
            .withPath("/submission/mobile-analytics")
            .withJson(
                analyticsPayloadFrom(
                    startDate = "2020-07-27T23:00:00Z",
                    endDate = "2020-07-28T22:59:00Z",
                    postDistrict = "AB10",
                    localAuthority = "E06000051"
                )
            )

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        expect {
            that(responseEvent) {
                status.isSameAs(METHOD_NOT_ALLOWED)
                body.isNull()
            }
        }
    }

    @Test
    fun `bad request when empty body`() {
        val responseEvent = responseFor("")
        assertStatusIs400(responseEvent)
    }

    @Test
    fun `bad request when malformed json`() {
        val responseEvent = responseFor("{")
        assertStatusIs400(responseEvent)
    }

    @Test
    fun `bad request when empty json object`() {
        val responseEvent = responseFor("{}")
        assertStatusIs400(responseEvent)
    }

    @Test
    fun `bad request when start date is in invalid format`() {
        val responseEvent = responseFor(
            analyticsPayloadFrom(
                startDate = "2020-06-2001:00:00Z",
                endDate = "2020-06-20T22:00:00Z",
                postDistrict = "AB10",
                localAuthority = "E06000051"
            )
        )
        assertStatusIs400(responseEvent)
    }

    @Test
    fun `bad request when end date is in invalid format`() {
        val responseEvent = responseFor(
            analyticsPayloadFrom(
                startDate = "2020-06-20T22:00:00Z",
                endDate = "2020-06-20T22:00:00",
                postDistrict = "AB10",
                localAuthority = "E06000051"
            )
        )
        assertStatusIs400(responseEvent)
    }

    private fun assertStatusIs400(responseEvent: APIGatewayProxyResponseEvent) {
        expect {
            that(responseEvent) {
                status.isSameAs(BAD_REQUEST)
                body.isNull()
            }
            that(events).contains(MobileAnalyticsSubmission::class, UnprocessableJson::class)
        }
    }

    private fun responseFor(requestPayload: String): APIGatewayProxyResponseEvent {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withPath("/submission/mobile-analytics")
            .withBody(requestPayload)

        return handler.handleRequest(requestEvent, ContextBuilder.aContext())
    }


    @Test
    fun `assert payload returns 200 and matches`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(analyticsPayloadFrom(
                startDate = "2020-01-27T23:00:00Z",
                endDate = "2020-01-28T22:59:00Z",
                postDistrict = "AB13",
                localAuthority = "E06000051"
            ))

        val slot = slot<PutRecordRequest>()

        every { kinesisFirehose.putRecord(capture(slot)) } answers { PutRecordResult() }

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        val exportedJson = String(slot.captured.record.data.array(), Charset.forName("UTF-8"))

        expect {
            that(responseEvent) {
                status.isSameAs(OK)
                body.isNull()
            }
            that(events).contains(MobileAnalyticsSubmission::class)
            that(exportedJson).isEqualToJson(TestData.STORED_ANALYTICS_PAYLOAD)
        }

    }

    companion object {
        fun analyticsPayloadFrom(
            startDate: String,
            endDate: String,
            postDistrict: String = "AB10",
            localAuthority: String? = null
        ) = """
            {
              "metadata": {
                "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                "latestApplicationVersion": "3.0",
                "deviceModel": "iPhone11,2",
                "postalDistrict": "$postDistrict"
                ${localAuthority?.let { ""","localAuthority": "$it"""" } ?: ""}
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
              },
              "includesMultipleApplicationVersions": false
            }
        """.trimIndent()

    }
}
