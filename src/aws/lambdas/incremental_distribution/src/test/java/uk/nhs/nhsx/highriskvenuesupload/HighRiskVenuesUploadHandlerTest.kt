package uk.nhs.nhsx.highriskvenuesupload

import com.amazonaws.HttpMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.testhelper.ContextBuilder
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.data.TestData.RISKY_VENUES_UPLOAD_PAYLOAD

class HighRiskVenuesUploadHandlerTest {

    private val service = mock(HighRiskVenuesUploadService::class.java)
    private val handler = HighRiskVenuesUploadHandler(TestEnvironments.TEST.apply(mapOf("MAINTENANCE_MODE" to "false")), { true }, service)

    @Test
    fun mapsOkResultToHttpResponse() {
        `when`(service.upload(any())).thenReturn(VenuesUploadResult.ok())
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.ACCEPTED_202.code)
        assertThat(responseEvent.body).isEqualTo("successfully uploaded")
        verify(service, times(1)).upload(RISKY_VENUES_UPLOAD_PAYLOAD)
    }

    @Test
    fun mapsValidationErrorResultToHttpResponse() {
        `when`(service.upload(any())).thenReturn(VenuesUploadResult.validationError("some-error"))
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code)
        assertThat(responseEvent.body).isEqualTo("some-error")
        verify(service, times(1)).upload(RISKY_VENUES_UPLOAD_PAYLOAD)
    }

    @Test
    fun notFoundWhenPathIsWrong() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("dodgy")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.NOT_FOUND_404.code)
        assertThat(responseEvent.body).isEqualTo(null)
        verifyNoInteractions(service)
    }

    @Test
    fun notAllowedWhenMethodIsWrong() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.METHOD_NOT_ALLOWED_405.code)
        assertThat(responseEvent.body).isEqualTo(null)
        verifyNoInteractions(service)
    }

    @Test
    fun unprocessableWhenWrongContentType() {
        val requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withJson("{}")
            .build()
        val responseEvent = handler.handleRequest(requestEvent, ContextBuilder.aContext())
        assertThat(responseEvent.statusCode).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code)
        assertThat(responseEvent.body).isEqualTo("validation error: Content type is not text/csv")
        verifyNoInteractions(service)
    }
}