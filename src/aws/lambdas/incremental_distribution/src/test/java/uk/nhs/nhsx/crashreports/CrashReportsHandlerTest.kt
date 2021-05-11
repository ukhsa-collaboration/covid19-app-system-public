package uk.nhs.nhsx.crashreports

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.events.IncomingHttpRequest
import uk.nhs.nhsx.core.events.MobileCrashReportsSubmission
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RequestRejected
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasBody
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus

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
        assertThat(responseEvent.statusCode).isEqualTo(200)
        events.containsExactly(
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
        assertThat(responseEvent.statusCode).isEqualTo(200)
        events.containsExactly(
            MobileCrashReportsSubmission::class,
            CrashReportStored::class,
            IncomingHttpRequest::class
        )

        val event = events.first { it is CrashReportStored } as CrashReportStored

        assertThat(event.crashReport).isEqualTo(
            CrashReportRequest(
                exception = "android.app.RemoteServiceException",
                threadName = "some     www.abc.com     message",
                stackTrace = "another message www.example.com"
            )
        )
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
        assertThat(responseEvent.statusCode).isEqualTo(200)
        events.containsExactly(
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

        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400))
        assertThat(responseEvent, hasBody(equalTo(null)))
        events.containsExactly(
            MobileCrashReportsSubmission::class,
            RequestRejected::class,
            IncomingHttpRequest::class
        )
    }

    @Test
    fun `not found when path is wrong`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withPath("invalid")
            .withJson("")
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404))
        assertThat(responseEvent, hasBody(equalTo(null)))
        events.containsExactly(IncomingHttpRequest::class)
    }

    @Test
    fun `not allowed when method is wrong`() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("something")
            .withPath("/submission/crash-reports")
            .withJson("")
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())

        assertThat(responseEvent, hasStatus(HttpStatusCode.METHOD_NOT_ALLOWED_405))
        assertThat(responseEvent, hasBody(equalTo(null)))
        events.containsExactly(IncomingHttpRequest::class)
    }

    private fun assertStatusIs400(responseEvent: APIGatewayProxyResponseEvent) {
        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400))
        assertThat(responseEvent, hasBody(equalTo(null)))
        events.containsExactly(
            MobileCrashReportsSubmission::class,
            UnprocessableJson::class,
            IncomingHttpRequest::class
        )
    }

    private fun responseFor(requestPayload: String): APIGatewayProxyResponseEvent {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withBearerToken("anything")
            .withPath("/submission/crash-reports")
            .withBody(requestPayload)
            .build()
        return handler.handleRequest(requestEvent, ContextBuilder.aContext())
    }
}
