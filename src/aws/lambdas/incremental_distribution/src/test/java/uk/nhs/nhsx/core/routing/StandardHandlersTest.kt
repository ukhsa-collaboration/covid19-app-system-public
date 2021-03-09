package uk.nhs.nhsx.core.routing

import com.amazonaws.HttpMethod.GET
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.apache.http.entity.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.events.IncomingHttpRequest
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RequestRejected
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.BAD_REQUEST_400
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.FORBIDDEN_403
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.OK_200
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.SERVICE_UNAVAILABLE_503
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS.Android
import uk.nhs.nhsx.core.headers.MobileOSVersion
import uk.nhs.nhsx.core.headers.UserAgent
import uk.nhs.nhsx.core.routing.Routing.RouterMatch.Matched
import uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy
import uk.nhs.nhsx.core.routing.StandardHandlers.expectingContentType
import uk.nhs.nhsx.core.routing.StandardHandlers.filteringWhileMaintenanceModeEnabled
import uk.nhs.nhsx.core.routing.StandardHandlers.loggingIncomingRequests
import uk.nhs.nhsx.core.routing.StandardHandlers.requiringAuthorizationHeader
import uk.nhs.nhsx.core.routing.StandardHandlers.requiringCustomAccessIdentity
import uk.nhs.nhsx.core.routing.StandardHandlers.signedBy
import uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.Companion.request
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasHeader
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
import java.time.Instant

class StandardHandlersTest {

    private val testContext = TestContext()
    private  val events = RecordingEvents()

    @Test
    fun `sign successful responses`() {
        val signer = ResponseSigner { _, response -> response.headers["signature"] = "signature" }
        val handler = signedBy(signer) { _, _ -> HttpResponses.ok() }
        val response = handler(request().build(), testContext)
        assertThat(response, hasHeader("signature", equalTo("signature")))
    }

    @Test
    fun `doesn't sign 403 responses`() {
        val signer = ResponseSigner { _, response -> response.headers["signature"] = "signature" }
        val handler = signedBy(signer) { _, _ -> HttpResponses.withStatusCode(FORBIDDEN_403) }
        val response = handler(request().build(), testContext)
        assertThat(response, not(hasHeader("signature")))
    }

    @Test
    fun `oai header is required to match when environment variable is set`() {
        val env = mapOf("custom_oai" to "bob")
        val handler = requiringCustomAccessIdentity(
            RecordingEvents(),
            TestEnvironments.TEST.apply(env)
        ) { _, _ -> HttpResponses.ok() }
        assertThat(handler(request().build(), testContext), hasStatus(FORBIDDEN_403))
        assertThat(handler(request().withHeader("x-custom-oai", "bob").build(), testContext), hasStatus(OK_200))
        assertThat(handler(request().withHeader("x-custom-oai", "jim").build(), testContext), hasStatus(FORBIDDEN_403))
        assertThat(handler(request().withHeader("x-custom-oai", "").build(), testContext), hasStatus(FORBIDDEN_403))
        assertThat(
            handler(request().withHeader("x-custom-oai-something", "bob").build(), testContext), hasStatus(
                FORBIDDEN_403
            )
        )
    }

    @Test
    fun `requiring authorizationHeader supplied`() {
        val handler = requiringAuthorizationHeader { _, _ -> HttpResponses.ok() }
        assertThat(handler(request().withHeader("authorization", "something").build(), testContext), hasStatus(OK_200))
    }

    @Test
    fun `requiring AuthorizationHeader not supplied`() {
        val handler = requiringAuthorizationHeader { _, _ -> HttpResponses.ok() }
        assertThat(handler(request().build(), testContext), hasStatus(FORBIDDEN_403))
    }

    @Test
    fun `authorisedBy notSupplied`() {
        val handler = authorisedBy({ true }, okRoutingHandler())
        assertThat(handler(request().build(), testContext), hasStatus(FORBIDDEN_403))
    }

    private fun okRoutingHandler() = object : Routing.RoutingHandler {
        override fun match(request: APIGatewayProxyRequestEvent) = Matched(this)
        override fun invoke(request: APIGatewayProxyRequestEvent, context: Context) = HttpResponses.ok()
    }

    @Test
    fun `authorisedBy authorised`() {
        val handler = authorisedBy({ true }, okRoutingHandler())
        assertThat(handler(request().withHeader("authorization", "something").build(), testContext), hasStatus(OK_200))
    }

    @Test
    fun `authorisedBy notAuthorised`() {
        val handler = authorisedBy({ false }, okRoutingHandler())
        assertThat(
            handler(request().withHeader("authorization", "something").build(), testContext),
            hasStatus(FORBIDDEN_403)
        )
    }

    @Test
    fun `authorisedBy match and handle`() {
        val request = request().build()
        val handler = authorisedBy({ true }, okRoutingHandler())

        when (val match = handler.match(request)) {
            is Matched -> assertThat(match.handler(request, testContext), hasStatus(FORBIDDEN_403))
            else -> fail { "match is not of type Matched" }
        }
    }

    @Test
    fun `expectingContentType suppliedCorrect`() {
        val handler = expectingContentType(ContentType.APPLICATION_JSON) { _, _ -> HttpResponses.ok() }
        assertThat(
            handler(request().withHeader("Content-Type", "application/json").build(), testContext),
            hasStatus(OK_200)
        )
        assertThat(
            handler(request().withHeader("Content-Type", "application/json;charset=utf-8").build(), testContext),
            hasStatus(
                OK_200
            )
        )
    }

    @Test
    fun `expectingContentType suppliedIncorrect`() {
        val handler = expectingContentType(ContentType.APPLICATION_JSON) { _, _ -> HttpResponses.ok() }
        assertThat(
            handler(request().withHeader("Content-Type", "text/html").build(), testContext), hasStatus(
                UNPROCESSABLE_ENTITY_422
            )
        )
        assertThat(
            handler(request().withHeader("Content-Type", "application/vnd.ms-excel").build(), testContext), hasStatus(
                UNPROCESSABLE_ENTITY_422
            )
        )
    }

    @Test
    fun `expectingContentType notSupplied`() {
        val handler = expectingContentType(ContentType.APPLICATION_JSON) { _, _ -> HttpResponses.ok() }
        assertThat(handler(request().build(), testContext), hasStatus(BAD_REQUEST_400))
    }

    @Test
    fun `filtering while maintenance mode enabled supplied True`() {
        val env = mapOf("MAINTENANCE_MODE" to "TRUE")
        val handler =
            filteringWhileMaintenanceModeEnabled(events, TestEnvironments.TEST.apply(env)) { _, _ -> HttpResponses.ok() }
        assertThat(handler(request().build(), testContext), hasStatus(SERVICE_UNAVAILABLE_503))
        events.containsExactly(RequestRejected::class)
    }

    @Test
    fun `filtering while maintenance mode enabled supplied False`() {
        val env = mapOf("MAINTENANCE_MODE" to "FALSE")
        val handler =
            filteringWhileMaintenanceModeEnabled(events, TestEnvironments.TEST.apply(env)) { _, _ -> HttpResponses.ok() }
        assertThat(handler(request().build(), testContext), hasStatus(OK_200))
        events.containsExactly()
    }

    @Test
    fun `filtering while maintenance mode enabled supplied Empty`() {
        val env = mapOf("MAINTENANCE_MODE" to "")
        val handler =
            filteringWhileMaintenanceModeEnabled(events, TestEnvironments.TEST.apply(env)) { _, _ -> HttpResponses.ok() }
        assertThat(handler(request().build(), testContext), hasStatus(OK_200))
        events.containsExactly()
    }

    @Test
    fun `without signed response`() {
        val events = RecordingEvents()
        val environment = TestEnvironments.TEST.apply(
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
            .build()

        handler(request, testContext)

        events.containsExactly(IncomingHttpRequest::class)
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
            .build()

        handler(request, testContext)

        val incoming = events.first() as IncomingHttpRequest
        assertThat(incoming.uri, equalTo("/hello"))
        assertThat(incoming.method, equalTo("GET"))
        assertThat(incoming.apiKey, equalTo("none"))
        assertThat(
            incoming.userAgent,
            equalTo(UserAgent(MobileAppVersion.Version(4, 3, 0), Android, MobileOSVersion.of("29")))
        )
        assertThat(incoming.latency, equalTo(0))
        assertThat(incoming.requestId, equalTo("none"))
        assertThat(incoming.status, equalTo(200))
        assertThat(incoming.message, equalTo("Received http request: method=GET,path=/hello,requestId=none,apiKeyName=none,userAgent=p=Android,o=29,v=4.3.0,b=138,status=200,latency=PT0S"))
    }
}
