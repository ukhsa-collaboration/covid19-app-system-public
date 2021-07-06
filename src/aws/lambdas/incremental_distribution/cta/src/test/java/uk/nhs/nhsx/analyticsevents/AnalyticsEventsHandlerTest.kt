package uk.nhs.nhsx.analyticsevents

import com.amazonaws.HttpMethod
import com.amazonaws.HttpMethod.POST
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import smoke.data.AnalyticsEventsData.analyticsEvents
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.AwsResponseSigner
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.signature.KeyId
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner
import uk.nhs.nhsx.core.signature.Signature
import uk.nhs.nhsx.core.signature.Signer
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId
import java.util.*

class AnalyticsEventsHandlerTest {

    private val contentSigner = Signer {
        Signature(
            KeyId.of("some-id"),
            SigningAlgorithmSpec.ECDSA_SHA_256,
            "TEST_SIGNATURE".toByteArray()
        )
    }

    private val events = RecordingEvents()

    private val signer = AwsResponseSigner(RFC2616DatedSigner(SystemClock.CLOCK, contentSigner), events)

    private val handler = AnalyticsEventsHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI",
                "SUBMISSION_STORE" to "store",
                "ACCEPT_REQUESTS_ENABLED" to "true"
            )
        ),
        SystemClock.CLOCK,
        RecordingEvents(),
        { true },
        signer,
        FakeS3(),
        { ObjectKey.of("foo") },
        { true }
    )

    @Test
    fun `exposure window payload returns success`() {
        val request = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics-events")
            .withBearerToken("anything")
            .withBody(analyticsEvents(UUID.randomUUID()).trimIndent())
        val response = handler.handleRequest(request, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(200)
    }

    @Test
    fun `missing json fields returns bad request`() {
        val request = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics-events")
            .withBearerToken("anything")
            .withBody(
                """
                {
                    "metadata": {
                        "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                        "latestApplicationVersion": "3.0",
                        "deviceModel": "iPhone11,2"
                    },
                    "events": [
                        {
                            "type": "exposure_window",
                            "version": 1,
                            "payload": {
                                "date": "2020-08-24T21:59:00Z",
                                "infectiousness": "high|none|standard",
                                "scanInstances": [
                                    {
                                        "minimumAttenuation": 1,
                                        "secondsSinceLastScan": 5,
                                        "typicalAttenuation": 2
                                    }
                                ],
                                "riskScore": "FIXME: sample int value (range?) or string value (enum?)"
                            }
                        }
                    ]
                }
        """.trimIndent())

        val response = handler.handleRequest(request, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(400)
    }

    @Test
    fun `empty json payload returns bad request`() {
        val request = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics-events")
            .withBearerToken("anything")
            .withBody("{}")

        val response = handler.handleRequest(request, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(400)
    }

    @Test
    fun `no json payload returns bad request`() {
        val request = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics-events")
            .withBearerToken("anything")

        val response = handler.handleRequest(request, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(400)
    }

    @Test
    fun `http get not allowed`() {
        val request = request()
            .withMethod(HttpMethod.GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics-events")
            .withBearerToken("anything")

        val response = handler.handleRequest(request, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(405)
    }

    @Test
    fun `accept requests disabled`() {
        val request = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/submission/mobile-analytics-events")
            .withBearerToken("anything")

        val handler = AnalyticsEventsHandler(
            TestEnvironments.TEST.apply(
                mapOf(
                    "MAINTENANCE_MODE" to "false",
                    "SUBMISSION_STORE" to "store",
                    "ACCEPT_REQUESTS_ENABLED" to "false",
                    "custom_oai" to "OAI",
                )
            ),
            SystemClock.CLOCK,
            RecordingEvents(),
            { true },
            signer,
            FakeS3(),
            { ObjectKey.of("foo") },
            { true }
        )

        val response = handler.handleRequest(request, ContextBuilder.aContext())

        assertThat(response.statusCode).isEqualTo(503)
    }

}
