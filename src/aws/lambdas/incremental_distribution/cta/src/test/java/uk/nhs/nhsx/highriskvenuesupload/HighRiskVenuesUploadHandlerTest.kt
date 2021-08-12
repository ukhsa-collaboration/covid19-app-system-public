package uk.nhs.nhsx.highriskvenuesupload

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.UNPROCESSABLE_ENTITY
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RiskyVenuesUpload
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyResponse.status
import uk.nhs.nhsx.testhelper.assertions.contains
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.data.TestData.RISKY_VENUES_UPLOAD_PAYLOAD
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withCsv
import uk.nhs.nhsx.testhelper.withCustomOai
import uk.nhs.nhsx.testhelper.withJson
import uk.nhs.nhsx.testhelper.withMethod
import uk.nhs.nhsx.testhelper.withRequestId

class HighRiskVenuesUploadHandlerTest {

    private val service = mockk<HighRiskVenuesUploadService>()
    private val events = RecordingEvents()
    private val handler = HighRiskVenuesUploadHandler(
        environment = TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ),
        clock = SystemClock.CLOCK,
        events = events,
        authenticator = { true },
        service = service,
        healthAuthenticator = { true }
    )

    @Test
    fun `maps ok result to http response`() {
        every { service.upload(any()) } returns VenuesUploadResult.ok()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(ACCEPTED)
            body.isEqualTo("successfully uploaded")
        }

        verify(exactly = 1) { service.upload(RISKY_VENUES_UPLOAD_PAYLOAD) }

        expectThat(events).contains(RiskyVenuesUpload::class)
    }

    @Test
    fun `maps validation error result to http response`() {
        every { service.upload(any()) } returns VenuesUploadResult.validationError("some-error")

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
            body.isEqualTo("some-error")
        }

        verify(exactly = 1) { service.upload(RISKY_VENUES_UPLOAD_PAYLOAD) }

        expectThat(events).contains(RiskyVenuesUpload::class, HighRiskVenueUploadFileInvalid::class)
    }

    @Test
    fun `not found when path is wrong`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("dodgy")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(NOT_FOUND)
            body.isNull()
        }
    }

    @Test
    fun `not allowed when method is wrong`() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(METHOD_NOT_ALLOWED)
            body.isNull()
        }
    }

    @Test
    fun `unprocessable when wrong content type`() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withJson("{}")

        val responseEvent = handler.handleRequest(requestEvent, aContext())

        expectThat(responseEvent) {
            status.isSameAs(UNPROCESSABLE_ENTITY)
            body.isEqualTo("validation error: Content type is not text/csv")
        }
    }
}
