package uk.nhs.nhsx.circuitbreakers

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec.ECDSA_SHA_256
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
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.Parameter
import uk.nhs.nhsx.core.events.CircuitBreakerExposureRequest
import uk.nhs.nhsx.core.events.CircuitBreakerExposureResolution
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
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId

class ExposureNotificationHandlerTest {

    private val contentSigner = Signer {
        Signature(KeyId.of("some-id"), ECDSA_SHA_256, "TEST_SIGNATURE".toByteArray())
    }

    private val events = RecordingEvents()

    private val signer = AwsResponseSigner(RFC2616DatedSigner(SystemClock.CLOCK, contentSigner), events)

    private val initial = Parameter { PENDING }
    private val poll = Parameter { YES }

    private val breaker = CircuitBreakerService(initial, poll)
    private val handler = ExposureNotificationHandler(
        environment = TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ),
        clock = SystemClock.CLOCK,
        events = events,
        authenticator = { true },
        parameters = AwsSsmParameters(),
        signer = signer,
        circuitBreakerService = breaker,
        healthAuthenticator = { true }
    )

    @Test
    fun `handle circuit breaker request success`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson(
                """
                {
                    "matchedKeyCount": 2,
                    "daysSinceLastExposure": 3,
                    "maximumRiskScore": 150.123456
                }
                """.trimIndent()
            )

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.withReadJsonOrThrow<TokenResponse> {
                get(TokenResponse::approval).isEqualTo(PENDING.statusName)
                get(TokenResponse::approvalToken).matches(Regex("[a-zA-Z0-9]+"))
            }
        }

        expectThat(events).contains(CircuitBreakerExposureRequest::class)
    }

    @Test
    fun `handle circuit breaker request success with extra risk calculation score field`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson(
                """
                {
                    "matchedKeyCount": 2,
                    "daysSinceLastExposure": 3,
                    "maximumRiskScore": 150.123456,
                    "riskCalculationVersion": 8
                }
                """.trimIndent()
            )

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.withReadJsonOrThrow<TokenResponse> {
                get(TokenResponse::approval).isEqualTo(PENDING.statusName)
                get(TokenResponse::approvalToken).matches(Regex("[a-zA-Z\\d]{50}"))
            }
        }

        expectThat(events).contains(CircuitBreakerExposureRequest::class)
    }

    @Test
    fun `handle circuit breaker request invalid json data`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson("""{"invalidField": null}""")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
            headers.containsKey("x-amz-meta-signature")
        }
    }

    @Test
    fun `handle circuit breaker request invalid json format`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")
            .withJson("{ invalid }")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
            headers.containsKey("x-amz-meta-signature")
        }
    }

    @Test
    fun `handle circuit breaker request no body`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/request")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
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

        expectThat(response) {
            status.isSameAs(NOT_FOUND)
        }
    }

    @Test
    fun `handle circuit breaker missing token`() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/resolution")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
        }
    }

    @Test
    fun `handle circuit breaker resolution success`() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/circuit-breaker/exposure-notification/resolution/QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg")
            .withBearerToken("anything")

        val response = handler.handleRequest(requestEvent, aContext())

        expectThat(response) {
            status.isSameAs(OK)
            headers.containsKey("x-amz-meta-signature")
            body.withReadJsonOrThrow<ResolutionResponse> {
                get(ResolutionResponse::approval).isEqualTo(YES.statusName)
            }
        }

        expectThat(events).contains(CircuitBreakerExposureResolution::class)
    }
}
