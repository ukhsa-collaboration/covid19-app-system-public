package uk.nhs.nhsx.highriskpostcodesupload;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.primitives.Bytes;
import org.junit.Test;
import uk.nhs.nhsx.ProxyRequestBuilder;
import uk.nhs.nhsx.activationsubmission.persist.Environment;
import uk.nhs.nhsx.analyticssubmission.FakeS3Storage;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static uk.nhs.nhsx.ContextBuilder.aContext;

public class RiskyPostCodesHandlerTest {

    private final String payload =
        "# postal_district_code, risk_indicator\n" +
            "\"CODE1\", \"H\"\n" +
            "\"CODE2\", \"M\"\n" +
            "\"CODE3\", \"L\"";


    @SuppressWarnings("serial")
	private final Map<String, String> environmentSettings = new HashMap<String, String>() {{
        put("BUCKET_NAME", "my-bucket");
        put("DISTRIBUTION_ID", "my-distribution");
        put("DISTRIBUTION_INVALIDATION_PATTERN", "invalidation-pattern");
    }};

    private final Environment environment = Environment.fromName("test", Environment.Access.TEST.apply(environmentSettings));

    private final AwsCloudFront awsCloudFront = mock(AwsCloudFront.class);
    private final FakeS3Storage s3Storage = new FakeS3Storage();
    private final RiskyPostCodesCsvParser csvParser = new RiskyPostCodesCsvParser();
    private final TestDatedSigner datedSigner = new TestDatedSigner("date");

    private final Handler handler = new Handler(environment, (h) -> true, datedSigner, s3Storage, awsCloudFront, csvParser);

    @Test
    public void acceptsPayload() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withCsv(payload)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent.getStatusCode()).isEqualTo(HttpStatusCode.ACCEPTED_202.code);
        assertThat(responseEvent.getBody()).isEqualTo("successfully uploaded");

        String contentToStore = "{\"postDistricts\":{\"CODE2\":\"M\",\"CODE1\":\"H\",\"CODE3\":\"L\"}}";
        assertThat(datedSigner.count).isEqualTo(1);
        assertThat(datedSigner.content).isEqualTo(Bytes.concat("date:".getBytes(StandardCharsets.UTF_8), contentToStore.getBytes(StandardCharsets.UTF_8)));
        assertThat(s3Storage.count).isEqualTo(1);
        assertThat(s3Storage.bucket.value).isEqualTo("my-bucket");

        verify(awsCloudFront, times(1)).invalidateCache("my-distribution", "invalidation-pattern");
    }

    @Test
    public void notFoundWhenPathIsWrong() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("dodgy")
            .withBearerToken("anything")
            .withCsv(payload)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent.getStatusCode()).isEqualTo(HttpStatusCode.NOT_FOUND_404.code);
        assertThat(responseEvent.getBody()).isEqualTo(null);
        verifyNoMockInteractions();
    }

    @Test
    public void notAllowedWhenMethodIsWrong() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withCsv(payload)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent.getStatusCode()).isEqualTo(HttpStatusCode.METHOD_NOT_ALLOWED_405.code);
        assertThat(responseEvent.getBody()).isEqualTo(null);
        verifyNoMockInteractions();
    }

    @Test
    public void unprocessableWhenWrongContentType() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withJson("{}")
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertValidationError(responseEvent, "Content type is not text/csv");
    }

    @Test
    public void unprocessableWhenNoBody() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withCsv(null)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertValidationError(responseEvent, "validation error: No payload");
    }

    @Test
    public void unprocessableWhenEmptyBody() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withCsv("")
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertValidationError(responseEvent, "validation error: No payload");
    }

    @Test
    public void unprocessableWhenWrongHeader() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/upload/high-risk-postal-districts")
            .withBearerToken("anything")
            .withCsv("# risk_indicator, postal_district_code")
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertValidationError(responseEvent, "validation error: Invalid header");
    }

    private void assertValidationError(APIGatewayProxyResponseEvent responseEvent, String reason) {
        assertThat(responseEvent.getStatusCode()).isEqualTo(HttpStatusCode.UNPROCESSABLE_ENTITY_422.code);
        assertThat(responseEvent.getBody()).isEqualTo(reason);
        verifyNoMockInteractions();
    }

    private void verifyNoMockInteractions() {
        verifyNoInteractions(awsCloudFront);
    }
}