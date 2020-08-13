package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONParser;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;
import uk.nhs.nhsx.testkitorder.TestOrderResponseFactory.TestKitRequestType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestKitOrderServiceTest {

    @Test
    public void asJson() throws Exception {
        TestResult testResult = new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available");
        String json = TestKitOrderService.asJsonResponse(testResult);
        JSONAssert.assertEquals("{\"testEndDate\": \"2020-04-23T18:34:03Z\", \"testResult\": \"POSITIVE\"}", json, JSONCompareMode.STRICT);
    }

    @Test
    public void invalidDateThrowsException() {
        TestResult testResult = new TestResult("abc", "2020-04-23-ZZZ", "POSITIVE", "available");
        Assertions.assertThatThrownBy(() -> TestKitOrderService.asJsonResponse(testResult))
                .isInstanceOf(ApiResponseException.class)
                .hasMessage("Unexpected date format");
    }

    @Test
    public void invalidTestResultThrowsException() {
        TestResult testResult = new TestResult("abc", "2020-04-23T18:34:03Z", "A", "available");
        Assertions.assertThatThrownBy(() -> TestKitOrderService.asJsonResponse(testResult))
                .isInstanceOf(ApiResponseException.class)
                .hasMessage("Unexpected test result");
    }

    @Test
    public void handleTestResultRequestSuccess() {
        TestKitOrderService service = new TestKitOrderService(
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
            ),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        TestResultPollingToken pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7");
        TestLookupRequest request = new TestLookupRequest(pollingToken);
        APIGatewayProxyResponseEvent response = service.handleTestResultRequest(request);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    public void handleTestResultRequestSuccessNoContent() {
        TestKitOrderService service = new TestKitOrderService(
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "pending")
            ),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        TestResultPollingToken pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7");
        TestLookupRequest request = new TestLookupRequest(pollingToken);
        APIGatewayProxyResponseEvent response = service.handleTestResultRequest(request);
        assertThat(response.getStatusCode()).isEqualTo(204);
    }

    @Test
    public void handleTestResultRequestThatDoesNotExist() {
        TestKitOrderService service = new TestKitOrderService(
            new MockTestKitOrderPersistenceService(null),
            new TestOrderResponseFactory(null, null),
            new TokensGenerator()
        );

        TestResultPollingToken pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7");
        TestLookupRequest request = new TestLookupRequest(pollingToken);
        assertThatThrownBy(() -> service.handleTestResultRequest(request))
            .isInstanceOf(ApiResponseException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    public void handleTestOrderRequestSuccess() throws Exception {
        TestKitOrderService service = new TestKitOrderService(
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
            ),
            new TestOrderResponseFactory("https://example.order-a-test.uk", "https://example.register-a-test.uk"),
            new TokensGenerator()
        );
        APIGatewayProxyResponseEvent response = service.handleTestOrderRequest(TestKitRequestType.ORDER);
        assertThat(response.getStatusCode()).isEqualTo(200);

        JSONObject jsonObject = (JSONObject) JSONParser.parseJSON(response.getBody());
        assertThat((String) jsonObject.get("diagnosisKeySubmissionToken")).isNotBlank();
        assertThat((String) jsonObject.get("testResultPollingToken")).isNotBlank();
        assertThat((String) jsonObject.get("tokenParameterValue")).matches("[a-z0-9]{8}");
        assertThat((String) jsonObject.get("websiteUrlWithQuery")).matches("https://example\\.order-a-test\\.uk\\?ctaToken=[a-z0-9]{8}");
    }

    @Test
    public void handleTestRegisterRequestSuccess() throws Exception {
        TestKitOrderService service = new TestKitOrderService(
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
            ),
            new TestOrderResponseFactory("https://example.order-a-test.uk", "https://example.register-a-test.uk"),
            new TokensGenerator()
        );
        APIGatewayProxyResponseEvent response = service.handleTestOrderRequest(TestKitRequestType.REGISTER);
        assertThat(response.getStatusCode()).isEqualTo(200);

        JSONObject jsonObject = (JSONObject) JSONParser.parseJSON(response.getBody());
        assertThat((String) jsonObject.get("diagnosisKeySubmissionToken")).isNotBlank();
        assertThat((String) jsonObject.get("testResultPollingToken")).isNotBlank();
        assertThat((String) jsonObject.get("tokenParameterValue")).matches("[a-z0-9]{8}");
        assertThat((String) jsonObject.get("websiteUrlWithQuery")).matches("https://example\\.register-a-test\\.uk\\?ctaToken=[a-z0-9]{8}");
    }

    @Test
    public void handleTestRegisterError() {
        TestKitOrderService service = new TestKitOrderService(
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
            ),
            new TestOrderResponseFactory("https://example.order-a-test.uk", "https://example.register-a-test.uk"),
            new TokensGenerator()
        );

        assertThatThrownBy(() -> service.handleTestOrderRequest(null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Did not specify a test kit request type");
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
