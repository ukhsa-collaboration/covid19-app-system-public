package uk.nhs.nhsx.highriskvenuesupload

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RiskyVenuesUpload
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
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
        TestEnvironments.TEST.apply(mapOf(
            "MAINTENANCE_MODE" to "false",
            "custom_oai" to "OAI")
        ),
        SystemClock.CLOCK,
        events,
        { true },
        service,
        { true }
    )

    @Test
    fun mapsOkResultToHttpResponse() {
        every { service.upload(any()) } returns VenuesUploadResult.ok()

        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.ACCEPTED_202.code)
        assertThat(responseEvent.body).isEqualTo("successfully uploaded")

        verify(exactly = 1) { service.upload(RISKY_VENUES_UPLOAD_PAYLOAD) }

        events.contains(RiskyVenuesUpload::class)
    }

    @Test
    fun mapsValidationErrorResultToHttpResponse() {
        every { service.upload(any()) } returns VenuesUploadResult.validationError("some-error")
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code)
        assertThat(responseEvent.body).isEqualTo("some-error")
        verify(exactly = 1) { service.upload(RISKY_VENUES_UPLOAD_PAYLOAD) }
        events.contains(RiskyVenuesUpload::class, HighRiskVenueUploadFileInvalid::class)
    }

    @Test
    fun notFoundWhenPathIsWrong() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("dodgy")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.NOT_FOUND_404.code)
        assertThat(responseEvent.body).isEqualTo(null)
    }

    @Test
    fun notAllowedWhenMethodIsWrong() {
        val requestEvent = request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.METHOD_NOT_ALLOWED_405.code)
        assertThat(responseEvent.body).isEqualTo(null)
    }

    @Test
    fun unprocessableWhenWrongContentType() {
        val requestEvent = request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withJson("{}")

        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code)
        assertThat(responseEvent.body).isEqualTo("validation error: Content type is not text/csv")
    }
}
