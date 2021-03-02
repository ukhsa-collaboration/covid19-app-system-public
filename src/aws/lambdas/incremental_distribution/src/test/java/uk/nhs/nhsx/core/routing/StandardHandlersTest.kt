package uk.nhs.nhsx.core.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.apache.http.entity.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.BAD_REQUEST_400
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.FORBIDDEN_403
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.OK_200
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.SERVICE_UNAVAILABLE_503
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422
import uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy
import uk.nhs.nhsx.core.routing.StandardHandlers.expectingContentType
import uk.nhs.nhsx.core.routing.StandardHandlers.filteringWhileMaintenanceModeEnabled
import uk.nhs.nhsx.core.routing.StandardHandlers.requiringAuthorizationHeader
import uk.nhs.nhsx.core.routing.StandardHandlers.requiringCustomAccessIdentity
import uk.nhs.nhsx.core.routing.StandardHandlers.signedBy
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.Companion.request
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasHeader
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus

class StandardHandlersTest {

    @Test
    fun `sign successful responses`() {
        val signer = ResponseSigner { _, response -> response.headers["signature"] = "signature" }
        val handler = signedBy(signer) { HttpResponses.ok() }
        val response = handler.handle(request().build())
        assertThat(response, hasHeader("signature", equalTo("signature")))
    }

    @Test
    fun `doesn't sign 403 responses`() {
        val signer = ResponseSigner { _, response -> response.headers["signature"] = "signature" }
        val handler = signedBy(signer) { HttpResponses.withStatusCode(FORBIDDEN_403) }
        val response = handler.handle(request().build())
        assertThat(response, not(hasHeader("signature")))
    }

    @Test
    fun `oai header is required to match when environment variable is set`() {
        val env = mapOf("custom_oai" to "bob")
        val handler = requiringCustomAccessIdentity(RecordingEvents(), TestEnvironments.TEST.apply(env)) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(FORBIDDEN_403))
        assertThat(handler.handle(request().withHeader("x-custom-oai", "bob").build()), hasStatus(OK_200))
        assertThat(handler.handle(request().withHeader("x-custom-oai", "jim").build()), hasStatus(FORBIDDEN_403))
        assertThat(handler.handle(request().withHeader("x-custom-oai", "").build()), hasStatus(FORBIDDEN_403))
        assertThat(
            handler.handle(request().withHeader("x-custom-oai-something", "bob").build()), hasStatus(
                FORBIDDEN_403
            )
        )
    }

    @Test
    fun `requiring authorizationHeader supplied`() {
        val handler = requiringAuthorizationHeader { HttpResponses.ok() }
        assertThat(handler.handle(request().withHeader("authorization", "something").build()), hasStatus(OK_200))
    }

    @Test
    fun `requiring AuthorizationHeader not supplied`() {
        val handler = requiringAuthorizationHeader { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(FORBIDDEN_403))
    }

    @Test
    fun `authorisedBy notSupplied`() {
        val handler = authorisedBy({ true }, okRoutingHandler())
        assertThat(handler.handle(request().build()), hasStatus(FORBIDDEN_403))
    }

    private fun okRoutingHandler() = object : Routing.RoutingHandler {
        override fun match(request: APIGatewayProxyRequestEvent?) = Routing.RouterMatch.matched(this)
        override fun handle(request: APIGatewayProxyRequestEvent?) = HttpResponses.ok()
    }

    @Test
    fun `authorisedBy authorised`() {
        val handler = authorisedBy({ true }, okRoutingHandler())
        assertThat(handler.handle(request().withHeader("authorization", "something").build()), hasStatus(OK_200))
    }

    @Test
    fun `authorisedBy notAuthorised`() {
        val handler = authorisedBy({ false }, okRoutingHandler())
        assertThat(handler.handle(request().withHeader("authorization", "something").build()), hasStatus(FORBIDDEN_403))
    }

    @Test
    fun `authorisedBy match and handle`() {
        val request = request().build()
        val handler = authorisedBy({ true }, okRoutingHandler())
        assertThat(handler.match(request).handle(request), hasStatus(FORBIDDEN_403))
    }

    @Test
    fun `expectingContentType suppliedCorrect`() {
        val handler = expectingContentType(ContentType.APPLICATION_JSON) { HttpResponses.ok() }
        assertThat(handler.handle(request().withHeader("Content-Type", "application/json").build()), hasStatus(OK_200))
        assertThat(
            handler.handle(request().withHeader("Content-Type", "application/json;charset=utf-8").build()), hasStatus(
                OK_200
            )
        )
    }

    @Test
    fun `expectingContentType suppliedIncorrect`() {
        val handler = expectingContentType(ContentType.APPLICATION_JSON) { HttpResponses.ok() }
        assertThat(
            handler.handle(request().withHeader("Content-Type", "text/html").build()), hasStatus(
                UNPROCESSABLE_ENTITY_422
            )
        )
        assertThat(
            handler.handle(request().withHeader("Content-Type", "application/vnd.ms-excel").build()), hasStatus(
                UNPROCESSABLE_ENTITY_422
            )
        )
    }

    @Test
    fun `expectingContentType notSupplied`() {
        val handler = expectingContentType(ContentType.APPLICATION_JSON) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(BAD_REQUEST_400))
    }

    @Test
    fun `filtering while maintenance mode enabled supplied True`() {
        val env = mapOf("MAINTENANCE_MODE" to "TRUE")
        val handler = filteringWhileMaintenanceModeEnabled(TestEnvironments.TEST.apply(env)) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(SERVICE_UNAVAILABLE_503))
    }

    @Test
    fun `filtering while maintenance mode enabled supplied False`() {
        val env = mapOf("MAINTENANCE_MODE" to "FALSE")
        val handler = filteringWhileMaintenanceModeEnabled(TestEnvironments.TEST.apply(env)) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(OK_200))
    }

    @Test
    fun `filtering while maintenance mode enabled supplied Empty`() {
        val env = mapOf("MAINTENANCE_MODE" to "")
        val handler = filteringWhileMaintenanceModeEnabled(TestEnvironments.TEST.apply(env)) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(OK_200))
    }
}
