package uk.nhs.nhsx.virology;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.core.exceptions.TransactionException;
import uk.nhs.nhsx.virology.CountryTestKitWhitelist.CountryTestKitPair;
import uk.nhs.nhsx.virology.exchange.*;
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequest;
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequestV2;
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponse;
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2;
import uk.nhs.nhsx.virology.order.TokensGenerator;
import uk.nhs.nhsx.virology.order.VirologyOrderResponse;
import uk.nhs.nhsx.virology.order.VirologyRequestType;
import uk.nhs.nhsx.virology.order.VirologyWebsiteConfig;
import uk.nhs.nhsx.virology.persistence.*;
import uk.nhs.nhsx.virology.result.VirologyLookupResult;
import uk.nhs.nhsx.virology.result.VirologyResultRequest;
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest;
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse;

import java.time.Instant;
import java.time.Period;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;
import static uk.nhs.nhsx.virology.CountryTestKitWhitelist.isDiagnosisKeysSubmissionSupported;
import static uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLiveCalculator.DEFAULT_TTL;
import static uk.nhs.nhsx.virology.result.VirologyResultRequest.*;
import static uk.nhs.nhsx.virology.result.VirologyResultValidator.validateTestResult;

public class VirologyService {

    private static final Logger logger = LogManager.getLogger(VirologyService.class);
    private static final int MAX_CTA_EXCHANGE_RETRY_COUNT = 2;

    private final VirologyPersistenceService persistenceService;
    private final TokensGenerator tokensGenerator;
    private final Supplier<Instant> systemClock;

    public VirologyService(VirologyPersistenceService persistenceService,
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

    public VirologyLookupResult virologyLookupForV1(VirologyLookupRequest virologyLookupRequest) {
        return persistenceService
            .getTestResult(virologyLookupRequest.testResultPollingToken)
            .map(this::virologyLookupResultV1)
            .orElseGet(VirologyLookupResult.NotFound::new);
    }

    public VirologyLookupResult virologyLookupForV2(VirologyLookupRequestV2 request) {
        return persistenceService
            .getTestResult(request.testResultPollingToken)
            .map(it -> virologyLookupResultV2(it, request.country))
            .orElseGet(VirologyLookupResult.NotFound::new);
    }

    private VirologyLookupResult virologyLookupResultV1(TestResult testResult) {
        if (testResult.isAvailable()) {
            markTestDataForDeletion(testResult, DEFAULT_TTL.apply(systemClock));
            return new VirologyLookupResult.Available(
                new VirologyLookupResponse(
                    testResult.testEndDate,
                    testResult.testResult,
                    testResult.testKit
                )
            );
        }
        return new VirologyLookupResult.Pending();
    }

    private VirologyLookupResult virologyLookupResultV2(TestResult testResult, Country country) {
        if (testResult.isAvailable()) {
            markTestDataForDeletion(testResult, DEFAULT_TTL.apply(systemClock));
            return new VirologyLookupResult.AvailableV2(
                new VirologyLookupResponseV2(
                    testResult.testEndDate,
                    testResult.testResult,
                    testResult.testKit,
                    isDiagnosisKeysSubmissionSupported(CountryTestKitPair.of(country, testResult.testKit))
                )
            );
        }
        return new VirologyLookupResult.Pending();
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
        validateTestResult(testResult.testResult, testResult.testEndDate);

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
        validateTestResult(tokenGenRequest.testResult, tokenGenRequest.testEndDate);

        var expireAt = systemClock.get().plus(Period.ofWeeks(4)).getEpochSecond();

        var testOrder = persistenceService.persistTestOrderAndResult(
            tokensGenerator::generateVirologyTokens,
            expireAt,
            tokenGenRequest.testResult,
            tokenGenRequest.testEndDate,
            tokenGenRequest.testKit
        );

        logger.info("Token gen created ctaToken: {}", testOrder.ctaToken.value);

        return VirologyTokenGenResponse.of(testOrder.ctaToken.value);
    }

    public CtaExchangeResult exchangeCtaTokenForV1(CtaExchangeRequest request) {
        return persistenceService
            .getTestOrder(request.ctaToken)
            .filter(it -> it.downloadCounter < MAX_CTA_EXCHANGE_RETRY_COUNT)
            .map(testOrder -> persistenceService
                .getTestResult(testOrder.testResultPollingToken)
                .map(testResult -> mapToCtaExchangeResultV1(testOrder, testResult))
                .orElseGet(ctaTokenNotFound(testOrder)))
            .orElseGet(CtaExchangeResult.NotFound::new);
    }

    public CtaExchangeResult exchangeCtaTokenForV2(CtaExchangeRequestV2 request) {
        return persistenceService
            .getTestOrder(request.ctaToken)
            .filter(it -> it.downloadCounter < MAX_CTA_EXCHANGE_RETRY_COUNT)
            .map(testOrder -> persistenceService
                .getTestResult(testOrder.testResultPollingToken)
                .map(testResult -> mapToCtaExchangeResultV2(testOrder, testResult, request.country))
                .orElseGet(ctaTokenNotFound(testOrder)))
            .orElseGet(CtaExchangeResult.NotFound::new);
    }

    private CtaExchangeResult mapToCtaExchangeResultV1(TestOrder testOrder, TestResult testResult) {
        if (testResult.isAvailable()) {
            persistenceService.updateOnCtaExchange(testOrder, testResult, DEFAULT_TTL.apply(systemClock));
            var response = new CtaExchangeResponse(
                testOrder.diagnosisKeySubmissionToken.value,
                testResult.testResult,
                testResult.testEndDate,
                testResult.testKit);
            logger.info("Cta token exchange successful for ctaToken: {}", testOrder.ctaToken.value);
            return new CtaExchangeResult.Available(response);
        }
        return new CtaExchangeResult.Pending();
    }

    private CtaExchangeResult mapToCtaExchangeResultV2(TestOrder testOrder,
                                                       TestResult testResult,
                                                       Country country) {
        if (testResult.isAvailable()) {
            persistenceService.updateOnCtaExchange(testOrder, testResult, DEFAULT_TTL.apply(systemClock));
            var response = new CtaExchangeResponseV2(
                testOrder.diagnosisKeySubmissionToken.value,
                testResult.testResult,
                testResult.testEndDate,
                testResult.testKit,
                isDiagnosisKeysSubmissionSupported(CountryTestKitPair.of(country, testResult.testKit))
            );
            logger.info("Cta token exchange successful for ctaToken: {}", testOrder.ctaToken.value);
            return new CtaExchangeResult.AvailableV2(response);
        }
        return new CtaExchangeResult.Pending();
    }

    private Supplier<CtaExchangeResult> ctaTokenNotFound(TestOrder testOrder) {
        return () -> {
            logger.info("Cta exchange could not find virology result when exchanging " +
                "ctaToken:" + testOrder.ctaToken +
                ", pollingToken:" + testOrder.testResultPollingToken.value +
                ", submissionToken:" + testOrder.diagnosisKeySubmissionToken.value);
            return new CtaExchangeResult.NotFound();
        };
    }

}
