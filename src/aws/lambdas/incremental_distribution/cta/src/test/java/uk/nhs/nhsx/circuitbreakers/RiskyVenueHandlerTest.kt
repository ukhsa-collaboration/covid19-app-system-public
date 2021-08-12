package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNPROCESSABLE_ENTITY
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.isEqualTo
import strikt.assertions.matches
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus.PENDING
import uk.nhs.nhsx.circuitbreakers.ApprovalStatus.YES
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.aws.ssm.Parameter
import uk.nhs.nhsx.core.events.CircuitBreakerVenueRequest
import uk.nhs.nhsx.core.events.CircuitBreakerVenueResolution
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.headers
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.assertions.withReadJsonOrThrow
import uk.nhs.nhsx.testhelper.proxy
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId

class RiskyVenueHandlerTest {

    private val contentSigner = Signer {
        Signature(
            KeyId.of("some-id"),
            SigningAlgorithmSpec.ECDSA_SHA_256,
            "TEST_SIGNATURE".toByteArray()
        )
    }

    private val events = RecordingEvents()

    private val signer = AwsResponseSigner(RFC2616DatedSigner(SystemClock.CLOCK, contentSigner), events)

    private val initial = Parameter { PENDING }
    private val poll = Parameter { YES }

    private val breaker = CircuitBreakerService(initial, poll)

    private val handler = RiskyVenueHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ), SystemClock.CLOCK, events, { true }, proxy(), signer, breaker, { true }
    )

    @Test
    fun `handle circuit breaker request with venue id`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/request")
            .withBearerToken("anything")
            .withJson("""{"venueId": "MAX8CHR1"}""")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.withReadJsonOrThrow<TokenResponse> {
                get(TokenResponse::approval).isEqualTo(PENDING.statusName)
                get(TokenResponse::approvalToken).matches(Regex("[a-zA-Z0-9]+"))
            }
        }

        expectThat(events).contains(CircuitBreakerVenueRequest::class)
    }

    @Test
    fun `handle circuit breaker request invalid json data`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/request")
            .withBearerToken("anything")
            .withJson("""{"invalidField": null}""")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
        }
    }

    @Test
    fun `handle circuit breaker request no body`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/request")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
        }
    }

    @Test
    fun `handle circuit breaker no such path`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/unknown-feature")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response).status.isSameAs(NOT_FOUND)
    }

    @Test
    fun `handle circuit breaker missing token`() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/resolution")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response).status.isSameAs(UNPROCESSABLE_ENTITY)
    }

    @Test
    fun `handle circuit breaker resolution success`() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/venue/resolution/abc123")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.withReadJsonOrThrow<ResolutionResponse> {
                get(ResolutionResponse::approval).isEqualTo(YES.statusName)
            }
        }

        expectThat(events).contains(CircuitBreakerVenueResolution::class)
    }
}
