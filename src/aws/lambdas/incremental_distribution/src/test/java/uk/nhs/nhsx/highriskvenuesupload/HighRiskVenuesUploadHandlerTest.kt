package uk.nhs.nhsx.highriskvenuesupload

import com.amazonaws.HttpMethod.GET
import com.amazonaws.HttpMethod.POST
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RiskyVenuesUpload
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.data.TestData.RISKY_VENUES_UPLOAD_PAYLOAD

class HighRiskVenuesUploadHandlerTest {

    private val service = mockk<HighRiskVenuesUploadService>()
    private val events = RecordingEvents()
    private val handler = HighRiskVenuesUploadHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ), { true }, service,
        { true }, events
    )

    @Test
    fun mapsOkResultToHttpResponse() {
        every { service.upload(any()) } returns VenuesUploadResult.ok()

        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.ACCEPTED_202.code)
        assertThat(responseEvent.body).isEqualTo("successfully uploaded")

        verify(exactly = 1) { service.upload(RISKY_VENUES_UPLOAD_PAYLOAD) }

        events.containsExactly(RiskyVenuesUpload::class)
    }

    @Test
    fun mapsValidationErrorResultToHttpResponse() {
        every { service.upload(any()) } returns VenuesUploadResult.validationError("some-error")
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code)
        assertThat(responseEvent.body).isEqualTo("some-error")
        verify(exactly = 1) { service.upload(RISKY_VENUES_UPLOAD_PAYLOAD) }
        events.containsExactly(RiskyVenuesUpload::class, HighRiskVenueUploadFileInvalid::class)
    }

    @Test
    fun notFoundWhenPathIsWrong() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("dodgy")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.NOT_FOUND_404.code)
        assertThat(responseEvent.body).isEqualTo(null)
    }

    @Test
    fun notAllowedWhenMethodIsWrong() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(GET)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.METHOD_NOT_ALLOWED_405.code)
        assertThat(responseEvent.body).isEqualTo(null)
    }

    @Test
    fun unprocessableWhenWrongContentType() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(POST)
            .withCustomOai("OAI")
            .withRequestId()
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withJson("{}")
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code)
        assertThat(responseEvent.body).isEqualTo("validation error: Content type is not text/csv")
    }
}
