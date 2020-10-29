package uk.nhs.nhsx.virology;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.core.exceptions.TransactionException;
import uk.nhs.nhsx.virology.exchange.CtaExchangeRequest;
import uk.nhs.nhsx.virology.exchange.CtaExchangeResponse;
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult;
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequest;
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponse;
import uk.nhs.nhsx.virology.order.TokensGenerator;
import uk.nhs.nhsx.virology.order.VirologyOrderResponse;
import uk.nhs.nhsx.virology.order.VirologyRequestType;
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig;
import uk.nhs.nhsx.virology.persistence.*;
import uk.nhs.nhsx.virology.result.*;

import java.time.Instant;
import java.time.Period;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;
import static uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLiveCalculator.DEFAULT_TTL;
import static uk.nhs.nhsx.virology.result.VirologyResultRequest.*;

public class VirologyService {

    private static final int MAX_CTA_EXCHANGE_RETRY_COUNT = 2;

    private static final Logger logger = LogManager.getLogger(VirologyService.class);

    private final VirologyDynamoService persistenceService;
    private final TokensGenerator tokensGenerator;
    private final Supplier<Instant> systemClock;

    public VirologyService(VirologyDynamoService persistenceService,
                           TokensGenerator tokensGenerator,
                           Supplier<Instant> systemClock) {
        this.persistenceService = persistenceService;
        this.tokensGenerator = tokensGenerator;
        this.systemClock = systemClock;
    }

    public VirologyOrderResponse handleTestOrderRequest(VirologyWebsiteConfig websiteConfig,
                                                        VirologyRequestType virologyRequestType) {
        var testOrder = orderTest();
        var websiteUrl = websiteUrl(websiteConfig, testOrder, virologyRequestType);
        return new VirologyOrderResponse(websiteUrl, testOrder);
    }

    private TestOrder orderTest() {
        var expireAt = systemClock.get().plus(Period.ofWeeks(4)).getEpochSecond();
        return persistenceService.persistTestOrder(tokensGenerator::generateVirologyTokens, expireAt);
    }

    private String websiteUrl(VirologyWebsiteConfig virologyWebsiteConfig,
                              TestOrder testOrder,
                              VirologyRequestType virologyRequestType) {
        var ctaTokenParam = "?ctaToken=" + testOrder.ctaToken;

        if (virologyRequestType == VirologyRequestType.ORDER)
            return virologyWebsiteConfig.orderWebsite + ctaTokenParam;

        return virologyWebsiteConfig.registerWebsite + ctaTokenParam;
    }

    public VirologyLookupResult virologyLookupFor(VirologyLookupRequest virologyLookupRequest) {
        return persistenceService
            .getTestResult(virologyLookupRequest.testResultPollingToken)
            .map(testResult -> {
                if (testResult.isAvailable()) {
                    markTestDataForDeletion(testResult, DEFAULT_TTL.apply(systemClock));
                    return new VirologyLookupResult.Available(
                        new VirologyLookupResponse(
                            testResult.testEndDate,
                            testResult.testResult
                        )
                    );
                }
                return new VirologyLookupResult.Pending();
            })
            .orElseGet(VirologyLookupResult.NotFound::new);
    }

    private void markTestDataForDeletion(TestResult testResult, VirologyDataTimeToLive virologyDataTimeToLive) {
        try {
            persistenceService.markForDeletion(testResult, virologyDataTimeToLive);
        } catch (TransactionException e) {
            logger.warn(
                "Unable to mark test data for deletion due to transaction, " +
                    "pollingToken:" + testResult.testResultPollingToken, e
            );
        }
    }

    public VirologyResultPersistOperation acceptTestResult(VirologyResultRequest testResult) {
        VirologyResultValidator.validateTestResult(testResult.testResult, testResult.testEndDate);

        switch (testResult.testResult) {
            case NPEX_POSITIVE:
                var expireAt = systemClock.get().plus(Period.ofWeeks(4)).getEpochSecond();
                var positive = VirologyResultRequest.Positive.from(testResult);
                return persistenceService.persistPositiveTestResult(positive, expireAt);

            case NPEX_NEGATIVE:
            case NPEX_VOID:
                var nonPositive = VirologyResultRequest.NonPositive.from(testResult);
                return persistenceService.persistNonPositiveTestResult(nonPositive);

            default:
                throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test result value");
        }
    }

    public VirologyTokenGenResponse acceptTestResultGeneratingTokens(VirologyTokenGenRequest tokenGenRequest) {
        VirologyResultValidator.validateTestResult(tokenGenRequest.testResult, tokenGenRequest.testEndDate);

        var expireAt = systemClock.get().plus(Period.ofWeeks(4)).getEpochSecond();

        var testOrder = persistenceService.persistTestOrderAndResult(
            tokensGenerator::generateVirologyTokens,
            expireAt,
            tokenGenRequest.testResult,
            tokenGenRequest.testEndDate
        );

        logger.info("Token gen created ctaToken: {}", testOrder.ctaToken.value);

        return VirologyTokenGenResponse.of(testOrder.ctaToken.value);
    }

    public CtaExchangeResult exchangeCtaToken(CtaExchangeRequest ctaExchangeRequest) {
        return persistenceService
            .getTestOrder(ctaExchangeRequest.ctaToken)
            .filter(it -> it.downloadCounter < MAX_CTA_EXCHANGE_RETRY_COUNT)
            .map(this::mapToCtaExchangeResult)
            .orElseGet(CtaExchangeResult.NotFound::new);
    }

    private CtaExchangeResult mapToCtaExchangeResult(TestOrder testOrder) {
        return persistenceService
            .getTestResult(testOrder.testResultPollingToken)
            .map(testResult -> {
                if (testResult.isAvailable()) {
                    persistenceService.updateOnCtaExchange(testOrder, testResult, DEFAULT_TTL.apply(systemClock));
                    var response = new CtaExchangeResponse(
                        testOrder.diagnosisKeySubmissionToken.value,
                        testResult.testResult,
                        testResult.testEndDate
                    );
                    logger.info("Cta token exchange successful for ctaToken: {}", testOrder.ctaToken.value);
                    return new CtaExchangeResult.Available(response);
                }
                return new CtaExchangeResult.Pending();
            })
            .orElseGet(() -> {
                logger.info("Cta exchange could not find virology result when exchanging " +
                    "ctaToken:" + testOrder.ctaToken +
                    ", pollingToken:" + testOrder.testResultPollingToken.value +
                    ", submissionToken:" + testOrder.diagnosisKeySubmissionToken.value);
                return new CtaExchangeResult.NotFound();
            });
    }
}
