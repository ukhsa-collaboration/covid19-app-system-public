package uk.nhs.nhsx.testkitorder;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONParser;
import uk.nhs.nhsx.ProxyRequestBuilder;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.AwsResponseSigner;
import uk.nhs.nhsx.core.signature.KeyId;
import uk.nhs.nhsx.core.signature.RFC2616DatedSigner;
import uk.nhs.nhsx.core.signature.Signature;
import uk.nhs.nhsx.core.signature.Signer;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.nhs.nhsx.ContextBuilder.aContext;

public class TestKitOrderHandlerTest {
    private final Signer contentSigner = mock(Signer.class);
    private final AwsResponseSigner signer = new AwsResponseSigner(new RFC2616DatedSigner(SystemClock.CLOCK, contentSigner));

    @Before
    public void setUpMock() {
        when(contentSigner.sign(any())).thenReturn(
            new Signature(KeyId.of("TEST_KEY_ID"), SigningAlgorithmSpec.ECDSA_SHA_256, "TEST_SIGNATURE".getBytes()));
    }
    @Test
    public void handleTestResultRequestSuccess() throws Exception {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
            ),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"98cff3dd-882c-417b-a00a-350a205378c7\"}")
            .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
        JSONAssert.assertEquals("{\"testEndDate\":\"2020-04-23T18:34:03Z\",\"testResult\":\"POSITIVE\"}", response.getBody(), JSONCompareMode.STRICT);
    }

    @Test
    public void handleTestResultRequestSuccessNoContent() {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "pending")
            ),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"98cff3dd-882c-417b-a00a-350a205378c7\"}")
            .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(204);
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
        assertThat(response.getBody()).isBlank();
    }

    @Test
    public void handleTestResultRequestMissingToken() {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "pending")
            ),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"\"}")
            .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(422);
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
    }

    @Test
    public void handleTestResultRequestNullToken() {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "pending")
            ),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":null}")
            .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(422);
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
    }

    @Test
    public void handleEmptyTestResultRequestToken() {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "pending")
            ),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"\"}")
            .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(422);
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
    }

    @Test
    public void handleTestResultRequestThatDoesNotExist() {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(null),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"98cff3dd-882c-417b-a00a-350a205378c7\"}")
            .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
    }

    @Test
    public void handleTestResultRequestForIncorrectRequestJson() {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(null),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
                .withMethod(HttpMethod.POST)
                .withPath("/virology-test/results")
                .withBearerToken("anything")
                .withJson("{\"invalidField\":\"98cff3dd-882c-417b-a00a-350a205378c7\"}")
                .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(422);
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
    }

    @Test
    public void handleTestResultRequestForMissingBody() {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(null),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(422);
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
    }

    @Test
    public void handleTestOrderRequestSuccess() throws Exception {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
            ),
            new TestOrderResponseFactory("https://example.order-a-test.uk", "https://example.register-a-test.uk"),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/home-kit/order")
            .withBearerToken("anything")
            .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(200);

        JSONObject jsonObject = (JSONObject) JSONParser.parseJSON(response.getBody());
        assertThat((String) jsonObject.get("diagnosisKeySubmissionToken")).isNotBlank();
        assertThat((String) jsonObject.get("testResultPollingToken")).isNotBlank();
        assertThat((String) jsonObject.get("tokenParameterValue")).matches("[a-z0-9]{8}");
        assertThat((String) jsonObject.get("websiteUrlWithQuery")).matches("https://example\\.order-a-test\\.uk\\?ctaToken=[a-z0-9]{8}");
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
    }

    @Test
    public void handleTestRegisterRequestSuccess() throws Exception {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
            ),
            new TestOrderResponseFactory("https://example.order-a-test.uk", "https://example.register-a-test.uk"),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
                .withMethod(HttpMethod.POST)
                .withPath("/virology-test/home-kit/register")
                .withBearerToken("anything")
                .build();


        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(200);

        JSONObject jsonObject = (JSONObject) JSONParser.parseJSON(response.getBody());
        assertThat((String) jsonObject.get("diagnosisKeySubmissionToken")).isNotBlank();
        assertThat((String) jsonObject.get("testResultPollingToken")).isNotBlank();
        assertThat((String) jsonObject.get("tokenParameterValue")).matches("[a-z0-9]{8}");
        assertThat((String) jsonObject.get("websiteUrlWithQuery")).matches("https://example\\.register-a-test\\.uk\\?ctaToken=[a-z0-9]{8}");
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
    }

    @Test
    public void handleUnknownPath() {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
            ),
            new TestOrderResponseFactory("https://example.order-a-test.uk", "https://example.register-a-test.uk"),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
                .withMethod(HttpMethod.POST)
                .withPath("/unknown/path")
                .withBearerToken("anything")
                .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(headersOrEmpty(response)).containsKey("x-amz-meta-signature");
    }

    @Test
    public void handleTestResultUnknownException() {
        Handler handler = new Handler(
            (e) -> true,
            signer,
            new MockThrowsTestKitOrderPersistenceService(),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/virology-test/results")
            .withBearerToken("anything")
            .withJson("{\"testResultPollingToken\":\"98cff3dd-882c-417b-a00a-350a205378c7\"}")
            .build();

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, aContext());
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).isBlank();
    }

    private Map<String, String> headersOrEmpty(APIGatewayProxyResponseEvent response) {
        return Optional.ofNullable(response.getHeaders()).orElse(Collections.emptyMap());
    }

    class MockThrowsTestKitOrderPersistenceService implements TestKitOrderPersistenceService {

        @Override
        public Optional<TestResult> getTestResult(TestResultPollingToken testResultPollingToken) {
            throw new RuntimeException("persistence error");
        }

        @Override
        public void persistTestOrder(TokensGenerator.TestOrderTokens tokens) {
            throw new RuntimeException("persistence error");
        }
    }

    class MockTestKitOrderPersistenceService implements TestKitOrderPersistenceService {
        TestResult testResultItem;

        MockTestKitOrderPersistenceService(TestResult testResultItem) {
            this.testResultItem = testResultItem;
        }

        @Override
        public Optional<TestResult> getTestResult(TestResultPollingToken testResultPollingToken) {
            return Optional.ofNullable(testResultItem);
        }

        @Override
        public void persistTestOrder(TokensGenerator.TestOrderTokens tokens) {

        }
    }

}
