@file:Suppress("SameParameterValue")

package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isNull
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.events.MobileAnalyticsSubmission
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId

class AnalyticsSubmissionDelegateTest {

    private val events = RecordingEvents()
    private val kinesisFirehose = mockk<AmazonKinesisFirehose>()
    private val config = AnalyticsConfig("firehoseStreamName", firehoseIngestEnabled = true)
    private val service = mockk<AnalyticsSubmissionService>()
    private val handler = AnalyticsSubmissionQueuedHandler.AnalyticsSubmissionDelegate(
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
        analyticsConfig = config,
        service = service
    )

    @Test
    fun `not found when path is wrong`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withPath("dodgy")
            .withJson(analyticsSubmissionAndroid(localAuthority = "E06000051"))

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
            .withJson(analyticsSubmissionIos(localAuthority = "E06000051"))

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
            analyticsSubmissionAndroid(
                startDate = "2020-06-2001:00:00Z",
                endDate = "2020-06-20T22:00:00Z"
            )
        )
        assertStatusIs400(responseEvent)
    }

    @Test
    fun `bad request when end date is in invalid format`() {
        val responseEvent = responseFor(
            analyticsSubmissionIos(
                startDate = "2020-06-20T22:00:00Z",
                endDate = "2020-06-20T22:00:00",
            )
        )
        assertStatusIs400(responseEvent)
    }

    @Test
    fun `assert payload returns 200 and matches`() {
        every { service.accept(any()) } just Runs
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(analyticsSubmissionIos(localAuthority = "E06000051"))

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        expect {
            that(responseEvent) {
                status.isSameAs(OK)
                body.isNull()
            }
            that(events).contains(MobileAnalyticsSubmission::class)
        }
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
}
