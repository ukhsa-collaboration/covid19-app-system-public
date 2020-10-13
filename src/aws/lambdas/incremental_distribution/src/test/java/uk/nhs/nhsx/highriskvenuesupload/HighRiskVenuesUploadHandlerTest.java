package uk.nhs.nhsx.highriskvenuesupload;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.Test;
import uk.nhs.nhsx.ProxyRequestBuilder;
import uk.nhs.nhsx.core.TestEnvironments;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static uk.nhs.nhsx.ContextBuilder.aContext;
import static uk.nhs.nhsx.TestData.RISKY_VENUES_UPLOAD_PAYLOAD;

public class HighRiskVenuesUploadHandlerTest {

    private final HighRiskVenuesUploadService service = mock(HighRiskVenuesUploadService.class);
    private final Handler handler = new Handler(TestEnvironments.TEST.apply(Map.of("MAINTENANCE_MODE", "false")), (h) -> true, service);

    @Test
    public void mapsOkResultToHttpResponse() {
        when(service.upload(any())).thenReturn(VenuesUploadResult.ok());

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent.getStatusCode()).isEqualTo(HttpStatusCode.ACCEPTED_202.code);
        assertThat(responseEvent.getBody()).isEqualTo("successfully uploaded");
        verify(service, times(1)).upload(RISKY_VENUES_UPLOAD_PAYLOAD);
    }

    @Test
    public void mapsValidationErrorResultToHttpResponse() {
        when(service.upload(any())).thenReturn(VenuesUploadResult.validationError("some-error"));

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent.getStatusCode()).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code);
        assertThat(responseEvent.getBody()).isEqualTo("some-error");
        verify(service, times(1)).upload(RISKY_VENUES_UPLOAD_PAYLOAD);
    }

    @Test
    public void notFoundWhenPathIsWrong() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("dodgy")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent.getStatusCode()).isEqualTo(HttpStatusCode.NOT_FOUND_404.code);
        assertThat(responseEvent.getBody()).isEqualTo(null);
        verifyNoInteractions(service);
    }

    @Test
    public void notAllowedWhenMethodIsWrong() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withCsv(RISKY_VENUES_UPLOAD_PAYLOAD)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent.getStatusCode()).isEqualTo(HttpStatusCode.METHOD_NOT_ALLOWED_405.code);
        assertThat(responseEvent.getBody()).isEqualTo(null);
        verifyNoInteractions(service);
    }

    @Test
    public void unprocessableWhenWrongContentType() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/identified-risk-venues")
            .withBearerToken("anything")
            .withJson("{}")
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent.getStatusCode()).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code);
        assertThat(responseEvent.getBody()).isEqualTo("validation error: Content type is not text/csv");
        verifyNoInteractions(service);
    }
}