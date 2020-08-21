package uk.nhs.nhsx.testkitorder;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.testkitorder.order.TestOrderResponseFactory;
import uk.nhs.nhsx.testkitorder.order.TestOrderResponseFactory.TestKitRequestType;
import uk.nhs.nhsx.testkitorder.lookup.TestLookupRequest;
import uk.nhs.nhsx.testkitorder.lookup.TestLookupResponse;
import uk.nhs.nhsx.testkitorder.lookup.TestResult;
import uk.nhs.nhsx.testkitorder.order.TokensGenerator;

import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.Jackson.toJson;
import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.NOT_FOUND_404;

public class TestKitOrderService {

    private final TestKitOrderPersistenceService persistenceService;
    private final TestOrderResponseFactory responseFactory;
    private final TokensGenerator tokensGenerator;
    private final Supplier<Instant> systemClock;


    TestKitOrderService(
        TestKitOrderPersistenceService persistenceService,
        TestOrderResponseFactory responseFactory,
        TokensGenerator tokensGenerator,
        Supplier<Instant> systemClock
    ) {
        this.persistenceService = persistenceService;
        this.responseFactory = responseFactory;
        this.tokensGenerator = tokensGenerator;
        this.systemClock = systemClock;
    }


    public APIGatewayProxyResponseEvent handleTestOrderRequest(TestKitRequestType testKitRequestType) {
        var expireAt = systemClock.get().plus(Period.ofWeeks(4)).getEpochSecond();
        var tokens = persistenceService.persistTestOrder(tokensGenerator::generateTokens, expireAt);
        var orderResponse = responseFactory.createTestOrderResponse(tokens, testKitRequestType);
        var jsonResponse = Jackson.toJson(orderResponse);
        return HttpResponses.ok(jsonResponse);
    }

    public APIGatewayProxyResponseEvent handleTestResultRequest(TestLookupRequest testLookupRequest) {
        return persistenceService
            .getTestResult(testLookupRequest.testResultPollingToken)
            .map(it -> handleTestResult(testLookupRequest, it))
            .orElseThrow(() ->
                new ApiResponseException(NOT_FOUND_404,
                    "Test result lookup submitted for unknown testResultPollingToken"
                )
            );
    }

    private APIGatewayProxyResponseEvent handleTestResult(TestLookupRequest testLookupRequest, TestResult it) {
        if ("available".equals(it.status)) {
            markTestDataForDeletion(testLookupRequest);
            return HttpResponses.ok(asJsonResponse(it));
        }

        return HttpResponses.noContent();
    }

    private void markTestDataForDeletion(TestLookupRequest testLookupRequest) {
        Instant instant = systemClock.get();
        long testDataExpireAt = instant.plus(4, ChronoUnit.HOURS).getEpochSecond();
        long submissionDataExpireAt = instant.plus(4, ChronoUnit.DAYS).getEpochSecond();

        VirologyDataTimeToLive virologyDataTimeToLive = new VirologyDataTimeToLive(
            testLookupRequest.testResultPollingToken,
            testDataExpireAt,
            submissionDataExpireAt
        );

        persistenceService.markForDeletion(virologyDataTimeToLive);
    }

    static String asJsonResponse(TestResult testResult) {
        TestLookupResponse response = new TestLookupResponse(
            testResult.testEndDate,
            testResult.testResult
        );
        return toJson(response);
    }

}
