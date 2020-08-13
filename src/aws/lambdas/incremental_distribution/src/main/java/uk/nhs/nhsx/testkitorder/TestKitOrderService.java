package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.testkitorder.TestOrderResponseFactory.TestKitRequestType;

import static uk.nhs.nhsx.core.Jackson.toJson;
import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.NOT_FOUND_404;

public class TestKitOrderService {

    private final TestKitOrderPersistenceService persistenceService;
    private final TestOrderResponseFactory responseFactory;
    private final TokensGenerator tokensGenerator;

    TestKitOrderService(
        TestKitOrderPersistenceService persistenceService,
        TestOrderResponseFactory responseFactory,
        TokensGenerator tokensGenerator
    ) {
        this.persistenceService = persistenceService;
        this.responseFactory = responseFactory;
        this.tokensGenerator = tokensGenerator;
    }


    public APIGatewayProxyResponseEvent handleTestOrderRequest(TestKitRequestType testKitRequestType) {
        TokensGenerator.TestOrderTokens tokens = tokensGenerator.generateTokens();
        persistenceService.persistTestOrder(tokens);
        TestOrderResponse orderResponse = responseFactory.createTestOrderResponse(tokens, testKitRequestType);
        String jsonResponse = Jackson.toJson(orderResponse);
        return HttpResponses.ok(jsonResponse);
    }

    public APIGatewayProxyResponseEvent handleTestResultRequest(TestLookupRequest testLookupRequest) {
        return persistenceService
            .getTestResult(testLookupRequest.testResultPollingToken)
            .map(it -> {
                if ("available".equals(it.status)) {
                    return HttpResponses.ok(asJsonResponse(it));
                } else {
                    return HttpResponses.noContent();
                }
            })
            .orElseThrow(() -> new ApiResponseException(NOT_FOUND_404,
                "Test result lookup submitted for unknown testResultPollingToken"));
    }

    static String asJsonResponse(TestResult testResult) {
        TestLookupResponse response = new TestLookupResponse(
            testResult.testEndDate,
            testResult.testResult
        );
        return toJson(response);
    }

}
