package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONParser;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;
import uk.nhs.nhsx.testkitorder.lookup.TestLookupRequest;
import uk.nhs.nhsx.testkitorder.lookup.TestResult;
import uk.nhs.nhsx.testkitorder.order.TestOrderResponseFactory;
import uk.nhs.nhsx.testkitorder.order.TestOrderResponseFactory.TestKitRequestType;
import uk.nhs.nhsx.testkitorder.order.TokensGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TestKitOrderServiceTest {

    private final Instant now = Instant.ofEpochSecond(0);
    private final Supplier<Instant> clock = () -> now;
    private final TestKitOrderPersistenceService persistenceService = mock(TestKitOrderPersistenceService.class);
    private final TestOrderResponseFactory testOrderResponseFactory =
        new TestOrderResponseFactory(
            "https://example.order-a-test.uk",
            "https://example.register-a-test.uk"
        );

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
        when(persistenceService.getTestResult(any()))
            .thenReturn(Optional.of(new TestResult(
                "abc", "2020-04-23T18:34:03Z", "POSITIVE", "available"
            )));

        TestKitOrderService service = new TestKitOrderService(
            persistenceService,
            testOrderResponseFactory,
            new TokensGenerator(),
            clock
        );

        TestResultPollingToken pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7");
        TestLookupRequest request = new TestLookupRequest(pollingToken);
        APIGatewayProxyResponseEvent response = service.handleTestResultRequest(request);
        assertThat(response.getStatusCode()).isEqualTo(200);

        verify(persistenceService, times(1))
            .getTestResult(pollingToken);
        verify(persistenceService, times(1))
            .markForDeletion(new VirologyDataTimeToLive(
                pollingToken,
                Duration.ofHours(4).toMillis() / 1000,
                Duration.ofDays(4).toMillis() / 1000
            ));
    }

    @Test
    public void handleTestResultRequestSuccessNoContent() {
        TestKitOrderService service = new TestKitOrderService(
            new MockTestKitOrderPersistenceService(
                new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "pending")
            ),
            testOrderResponseFactory,
            new TokensGenerator(),
            SystemClock.CLOCK

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
            testOrderResponseFactory,
            new TokensGenerator(),
            SystemClock.CLOCK
        );

        TestResultPollingToken pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7");
        TestLookupRequest request = new TestLookupRequest(pollingToken);
        assertThatThrownBy(() -> service.handleTestResultRequest(request))
            .isInstanceOf(ApiResponseException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    public void handleTestOrderRequestSuccess() throws Exception {
        var persistenceService = new MockTestKitOrderPersistenceService(
            new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
        );
        var service = new TestKitOrderService(
            persistenceService,
            testOrderResponseFactory,
            new TokensGenerator(),
            clock
        );
        APIGatewayProxyResponseEvent response = service.handleTestOrderRequest(TestKitRequestType.ORDER);
        assertThat(response.getStatusCode()).isEqualTo(200);

        JSONObject jsonObject = (JSONObject) JSONParser.parseJSON(response.getBody());
        assertThat((String) jsonObject.get("diagnosisKeySubmissionToken")).isNotBlank();
        assertThat((String) jsonObject.get("testResultPollingToken")).isNotBlank();
        assertThat((String) jsonObject.get("tokenParameterValue")).matches("[a-z0-9]{8}");
        assertThat((String) jsonObject.get("websiteUrlWithQuery")).matches("https://example\\.order-a-test\\.uk\\?ctaToken=[a-z0-9]{8}");
        assertThat(persistenceService.expireAt).isEqualTo(Duration.ofDays(4 * 7).getSeconds());
    }

    @Test
    public void handleTestRegisterRequestSuccess() throws Exception {
        var persistenceService = new MockTestKitOrderPersistenceService(
            new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available")
        );
        var service = new TestKitOrderService(
            persistenceService,
            testOrderResponseFactory,
            new TokensGenerator(),
            clock

        );
        APIGatewayProxyResponseEvent response = service.handleTestOrderRequest(TestKitRequestType.REGISTER);
        assertThat(response.getStatusCode()).isEqualTo(200);

        JSONObject jsonObject = (JSONObject) JSONParser.parseJSON(response.getBody());
        assertThat((String) jsonObject.get("diagnosisKeySubmissionToken")).isNotBlank();
        assertThat((String) jsonObject.get("testResultPollingToken")).isNotBlank();
        assertThat((String) jsonObject.get("tokenParameterValue")).matches("[a-z0-9]{8}");
        assertThat((String) jsonObject.get("websiteUrlWithQuery")).matches("https://example\\.register-a-test\\.uk\\?ctaToken=[a-z0-9]{8}");
        assertThat(persistenceService.expireAt).isEqualTo(Duration.ofDays(4 * 7).getSeconds());
    }

    static class MockTestKitOrderPersistenceService implements TestKitOrderPersistenceService {
        TestResult testResultItem;
        private long expireAt;

        MockTestKitOrderPersistenceService(TestResult testResultItem) {
            this.testResultItem = testResultItem;
        }

        @Override
        public Optional<TestResult> getTestResult(TestResultPollingToken testResultPollingToken) {
            return Optional.ofNullable(testResultItem);
        }

        @Override
        public TokensGenerator.TestOrderTokens persistTestOrder(Supplier<TokensGenerator.TestOrderTokens> tokens,
                                                                long expireAt) {
            this.expireAt = expireAt;
            return tokens.get();
        }

        @Override
        public void markForDeletion(VirologyDataTimeToLive virologyDataTimeToLive) {

        }
    }

}
