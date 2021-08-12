package uk.nhs.nhsx.crashreports

import com.amazonaws.HttpMethod
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.apache.commons.lang3.RandomStringUtils
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.filterIsInstance
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.withFirst
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.events.IncomingHttpRequest
import uk.nhs.nhsx.core.events.MobileCrashReportsSubmission
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RequestRejected
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId

class CrashReportsHandlerTest {

    private val events = RecordingEvents()

    private val handler = CrashReportsHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ),
        events
    ) { true }

    @Test
    fun `200 matching exception type`() {
        val responseEvent = responseFor(
            """
            {
              "exception": "android.app.RemoteServiceException",
              "threadName": "MainThread",
              "stackTrace": "android.app.RemoteServiceException: Here is the elusive exception message that we really need to capture"
            }
        """.trimIndent()
        )
        expectThat(responseEvent).status.isSameAs(OK)
        expectThat(events).containsExactly(
            MobileCrashReportsSubmission::class,
            CrashReportStored::class,
            IncomingHttpRequest::class
        )
    }

    @Test
    fun `200 sanitising fields`() {
        val responseEvent = responseFor(
            """
            {
              "exception": "android.app.RemoteServiceException",
              "threadName": "some     http://www.abc.com     message",
              "stackTrace": "another message https://www.example.com"
            }
        """.trimIndent()
        )

        expectThat(responseEvent).status.isSameAs(OK)

        expectThat(events).containsExactly(
            MobileCrashReportsSubmission::class,
            CrashReportStored::class,
            IncomingHttpRequest::class
        )

        expectThat(events).filterIsInstance<CrashReportStored>().withFirst {
            get(CrashReportStored::crashReport).isEqualTo(
                CrashReportRequest(
                    exception = "android.app.RemoteServiceException",
                    threadName = "some     www.abc.com     message",
                    stackTrace = "another message www.example.com"
                )
            )
        }
    }

    @Test
    fun `200 preventing abuse when exception type is not recognised`() {
        val responseEvent = responseFor(
            """
            {
              "exception": "unknown",
              "threadName": "MainThread",
              "stackTrace": "some"
            }
        """.trimIndent()
        )

        expectThat(responseEvent).status.isSameAs(OK)

        expectThat(events).containsExactly(
            MobileCrashReportsSubmission::class,
            CrashReportNotRecognised::class,
            IncomingHttpRequest::class
        )
    }

    @Test
    fun `bad request when json is does not deserialize into corresponding type`() {
        val responseEvent = responseFor(
            """
            {
              "name": "bob"
            }
        """.trimIndent()
        )
        assertStatusIs400(responseEvent)
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
    fun `bad request when json is too long`() {
        val payload = RandomStringUtils.randomAlphabetic(15000)
        val responseEvent = responseFor(payload)

        expect {
            that(responseEvent) {
                status.isSameAs(BAD_REQUEST)
                body.isNull()
            }

            that(events).containsExactly(
                MobileCrashReportsSubmission::class,
                RequestRejected::class,
                IncomingHttpRequest::class
            )
        }
    }

    @Test
    fun `not found when path is wrong`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withPath("invalid")
            .withJson("")

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expect {
            that(responseEvent) {
                status.isSameAs(NOT_FOUND)
                body.isNull()
            }

            that(events).containsExactly(IncomingHttpRequest::class)
        }
    }

    @Test
    fun `not allowed when method is wrong`() {
        val requestEvent = request()
            .withMethod(HttpMethod.GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("something")
            .withPath("/submission/crash-reports")
            .withJson("")

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expect {
            that(responseEvent) {
                status.isSameAs(METHOD_NOT_ALLOWED)
                body.isNull()
            }

            that(events).containsExactly(IncomingHttpRequest::class)
        }
    }

    private fun assertStatusIs400(responseEvent: APIGatewayProxyResponseEvent) {
        expect {
            that(responseEvent) {
                status.isSameAs(BAD_REQUEST)
                body.isNull()
            }

            that(events).containsExactly(
                MobileCrashReportsSubmission::class,
                UnprocessableJson::class,
                IncomingHttpRequest::class
            )
        }
    }

    private fun responseFor(requestPayload: String): APIGatewayProxyResponseEvent {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withPath("/submission/crash-reports")
            .withBody(requestPayload)

        return handler.handleRequest(requestEvent, aContext())
    }
}
