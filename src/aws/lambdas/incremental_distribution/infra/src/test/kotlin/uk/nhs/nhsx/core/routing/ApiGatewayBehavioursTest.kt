package uk.nhs.nhsx.core.routing

import com.amazonaws.HttpMethod.GET
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.first
import strikt.assertions.hasEntry
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.events.IncomingHttpRequest
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RequestRejected
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.FORBIDDEN_403
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.headers.MobileOSVersion
import uk.nhs.nhsx.core.headers.UserAgent
import uk.nhs.nhsx.core.routing.Routing.RouterMatch.Matched
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.headers
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.isEmpty
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withHeader
import uk.nhs.nhsx.testhelper.withMethod
import java.time.Instant

class ApiGatewayBehavioursTest {

    private val testContext = TestContext()
    private val events = RecordingEvents()

    @Test
    fun `sign successful responses`() {
        val signer = ResponseSigner { _, response -> response.headers["signature"] = "signature" }
        val handler = signedBy(signer) { _, _ -> HttpResponses.ok() }
        val response = handler(request(), testContext)

        expectThat(response).headers.hasEntry("signature", "signature")
    }

    @Test
    fun `doesn't sign 403 responses`() {
        val signer = ResponseSigner { _, response -> response.headers["signature"] = "signature" }
        val handler = signedBy(signer) { _, _ -> HttpResponses.withStatusCode(FORBIDDEN_403) }
        val response = handler(request(), testContext)

        expectThat(response).headers.not().containsKey("signature")
    }

    @Test
    fun `oai header is required to match when environment variable is set`() {
        val env = mapOf("custom_oai" to "bob")
        val handler = requiringCustomAccessIdentity(
            RecordingEvents(),
            TEST.apply(env)
        ) { _, _ -> HttpResponses.ok() }

        expectThat(handler(request(), testContext)).status.isSameAs(FORBIDDEN)
        expectThat(handler(request().withHeader("x-custom-oai", "bob"), testContext)).status.isSameAs(OK)
        expectThat(handler(request().withHeader("x-custom-oai", "jim"), testContext)).status.isSameAs(FORBIDDEN)
        expectThat(handler(request().withHeader("x-custom-oai", ""), testContext)).status.isSameAs(FORBIDDEN)
        expectThat(handler(request().withHeader("x-custom-oai-something", "bob"), testContext)).status.isSameAs(
            FORBIDDEN
        )
    }

    @Test
    fun `requiring authorizationHeader supplied`() {
        val handler = requiringAuthorizationHeader { _, _ -> HttpResponses.ok() }
        expectThat(handler(request().withHeader("authorization", "something"), testContext)).status.isSameAs(OK)
    }

    @Test
    fun `requiring AuthorizationHeader not supplied`() {
        val handler = requiringAuthorizationHeader { _, _ -> HttpResponses.ok() }

        expectThat(handler(request(), testContext)).status.isSameAs(FORBIDDEN)
    }

    @Test
    fun `authorisedBy notSupplied`() {
        val handler = authorisedBy({ true }, okRoutingHandler())

        expectThat(handler(request(), testContext)).status.isSameAs(FORBIDDEN)
    }

    private fun okRoutingHandler() = object : Routing.RoutingHandler {
        override fun match(request: APIGatewayProxyRequestEvent) = Matched(this)
        override fun invoke(request: APIGatewayProxyRequestEvent, context: Context) = HttpResponses.ok()
    }

    @Test
    fun `authorisedBy authorised`() {
        val handler = authorisedBy({ true }, okRoutingHandler())

        expectThat(handler(request().withHeader("authorization", "something"), testContext)).status.isSameAs(OK)
    }

    @Test
    fun `authorisedBy notAuthorised`() {
        val handler = authorisedBy({ false }, okRoutingHandler())

        expectThat(handler(request().withHeader("authorization", "something"), testContext)).status.isSameAs(FORBIDDEN)
    }

    @Test
    fun `authorisedBy match and handle`() {
        val request = request()
        val handler = authorisedBy({ true }, okRoutingHandler())
        val match = handler.match(request)

        expectThat(match).isA<Matched>().and {
            get { handler(request, testContext) }.status.isSameAs(FORBIDDEN)
        }
    }

    @Test
    fun `filtering while maintenance mode enabled supplied True`() {
        val env = mapOf("MAINTENANCE_MODE" to "TRUE")
        val handler = filteringWhileMaintenanceModeEnabled(
            events,
            TEST.apply(env)
        ) { _, _ -> HttpResponses.ok() }

        expectThat(handler(request(), testContext)).status.isSameAs(SERVICE_UNAVAILABLE)
        expectThat(events).containsExactly(RequestRejected::class)
    }

    @Test
    fun `filtering while maintenance mode enabled supplied False`() {
        val env = mapOf("MAINTENANCE_MODE" to "FALSE")
        val handler = filteringWhileMaintenanceModeEnabled(
            events,
            TEST.apply(env)
        ) { _, _ -> HttpResponses.ok() }

        expectThat(handler(request(), testContext)).status.isSameAs(OK)
        expectThat(events).isEmpty()
    }

    @Test
    fun `filtering while maintenance mode enabled supplied Empty`() {
        val env = mapOf("MAINTENANCE_MODE" to "")
        val handler = filteringWhileMaintenanceModeEnabled(
            events,
            TEST.apply(env)
        ) { _, _ -> HttpResponses.ok() }

        expectThat(handler(request(), testContext)).status.isSameAs(OK)
        expectThat(events).isEmpty()
    }

    @Test
    fun `without signed response`() {
        val events = RecordingEvents()
        val environment = TEST.apply(
            mapOf(
                "custom_oai" to "OAI",
                "MAINTENANCE_MODE" to "FALSE"
            )
        )

        val handler = withoutSignedResponses(events, environment) { _, _ -> HttpResponses.ok() }

        val request = request()
            .withMethod(GET)
            .withPath("/hello")
            .withCustomOai("OAI")
            .withHeader("User-Agent", "Android")

        handler(request, testContext)

        expectThat(events).containsExactly(IncomingHttpRequest::class)
    }

    @Test
    fun `logs incoming requests`() {
        val events = RecordingEvents()

        val handler = loggingIncomingRequests(events, { _, _ -> HttpResponses.ok() }, { Instant.EPOCH })

        val request = request()
            .withMethod(GET)
            .withPath("/hello")
            .withCustomOai("OAI")
            .withHeader("User-Agent", "p=Android,o=29,v=4.3.0,b=138")

        handler(request, testContext)

        expectThat(events).first().isA<IncomingHttpRequest>().and {
            get(IncomingHttpRequest::uri).isEqualTo("/hello")
            get(IncomingHttpRequest::method).isEqualTo("GET")
            get(IncomingHttpRequest::apiKey).isEqualTo("none")
            get(IncomingHttpRequest::userAgent).isEqualTo(UserAgent(
                MobileAppVersion.Version(4, 3, 0),
                MobileOS.Android,
                MobileOSVersion.of("29")
            ))
            get(IncomingHttpRequest::latency).isEqualTo(0)
            get(IncomingHttpRequest::requestId).isEqualTo("none")
            get(IncomingHttpRequest::status).isEqualTo(200)
            get(IncomingHttpRequest::message).isEqualTo("Received http request: method=GET,path=/hello,requestId=none,apiKeyName=none,userAgent=p=Android,o=29,v=4.3.0,b=138,status=200,latency=PT0S")
        }
    }
}
