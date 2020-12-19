package uk.nhs.nhsx.isolationpayment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.DateFormatValidator;
import uk.nhs.nhsx.isolationpayment.model.*;

import java.time.Instant;
import java.time.Period;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class IsolationPaymentMobileService {
    
    private static final Logger logger = LogManager.getLogger(IsolationPaymentMobileService.class);

    private final Supplier<Instant> systemClock;
    private final Supplier<String> tokenGenerator;
    private final IsolationPaymentPersistence persistence;
    private final String isolationPaymentWebsite;
    private final int tokenExpiryInWeeks;
    private final List<String> countriesWhitelisted;
    private final String auditLogPrefix;

    public IsolationPaymentMobileService(Supplier<Instant> systemClock,
                                         Supplier<String> tokenGenerator,
                                         IsolationPaymentPersistence persistence,
                                         String isolationPaymentWebsite,
                                         int tokenExpiryInWeeks,
                                         List<String> countriesWhitelisted,
                                         String auditLogPrefix) {
        this.systemClock = systemClock;
        this.tokenGenerator = tokenGenerator;
        this.persistence = persistence;
        this.isolationPaymentWebsite = isolationPaymentWebsite;
        this.tokenExpiryInWeeks = tokenExpiryInWeeks;
        this.countriesWhitelisted = countriesWhitelisted;
        this.auditLogPrefix = auditLogPrefix;
    }

    public TokenGenerationResponse handleIsolationPaymentOrder(TokenGenerationRequest tokenGenerationRequest) {
        var isEnabled = countriesWhitelisted.contains(tokenGenerationRequest.country);
        if (!isEnabled) {
            logger.info("{} CreateToken failed: not enabled for country={}", auditLogPrefix, tokenGenerationRequest.country);
            return new TokenGenerationResponse(false);
        }

        var isolationToken = new IsolationToken();
        isolationToken.tokenId = tokenGenerator.get();
        isolationToken.tokenStatus = TokenStateInternal.INT_CREATED.value;
        isolationToken.createdTimestamp = systemClock.get().getEpochSecond();
        isolationToken.expireAt = systemClock.get().plus(Period.ofWeeks(tokenExpiryInWeeks)).getEpochSecond();

        try {
            persistence.insertIsolationToken(isolationToken);

            logger.info("{} CreateToken successful: ipcToken={}", auditLogPrefix, isolationToken);

            return new TokenGenerationResponse(true, isolationToken.tokenId);
        }
        catch (Exception e) {
            logger.info("{} CreateToken exception: !ipcToken={}", auditLogPrefix, isolationToken, e);
            throw new RuntimeException(e);
        }
    }

    public TokenUpdateResponse handleIsolationPaymentUpdate(TokenUpdateRequest tokenUpdateRequest) {
        var websiteUrlWithQuery = isolationPaymentWebsite + tokenUpdateRequest.ipcToken;

        Optional<IsolationToken> isolationToken;
        try {
            isolationToken = persistence.getIsolationToken(tokenUpdateRequest.ipcToken);
        }
        catch (Exception e) {
            logger.error("{} UpdateToken exception: tokenId={}", auditLogPrefix, tokenUpdateRequest.ipcToken, e);
            throw new RuntimeException(e);
        }

        if (!isolationToken.isPresent()) { //API contract: we don't report this back to the client
            logger.info("{} UpdateToken failed: token not found, tokenId={}. Returning redirectUrl={}", auditLogPrefix, tokenUpdateRequest.ipcToken, websiteUrlWithQuery);
        }
        else if (TokenStateInternal.INT_CREATED.value.equals(isolationToken.get().tokenStatus)) {
            IsolationToken updatedToken = IsolationToken.clonedToken(isolationToken.get());
            try {
                updatedToken.riskyEncounterDate = DateFormatValidator.toZonedDateTimeMaybe(tokenUpdateRequest.riskyEncounterDate)
                    .map(ChronoZonedDateTime::toEpochSecond)
                    .orElseThrow();
                updatedToken.isolationPeriodEndDate = DateFormatValidator.toZonedDateTimeMaybe(tokenUpdateRequest.isolationPeriodEndDate)
                    .map(ChronoZonedDateTime::toEpochSecond)
                    .orElseThrow();
                updatedToken.updatedTimestamp = systemClock.get().getEpochSecond();
                updatedToken.tokenStatus = TokenStateInternal.INT_UPDATED.value;
                updatedToken.expireAt = updatedToken.isolationPeriodEndDate;
            }
            catch (Exception e) {
                logger.error("{} UpdateToken exception: tokenId={}", auditLogPrefix, tokenUpdateRequest.ipcToken, e);
                throw new RuntimeException(e); //FIXME check API contract: return 400?
            }

            try {
                persistence.updateIsolationToken(updatedToken, TokenStateInternal.INT_CREATED);

                logger.info("{} UpdateToken successful: existing.ipcToken={}, updated.ipcToken={}. Returning redirectUrl={}", auditLogPrefix, isolationToken.get(), updatedToken, websiteUrlWithQuery);
            }
            catch (Exception e) {
                logger.error("{} UpdateToken exception: existing.ipcToken={}, !updated.ipcToken={}", auditLogPrefix, isolationToken.get(), updatedToken, e);
                throw new RuntimeException(e);
            }
        }
        else { //API contract: we don't report this back to the client
            logger.info("{} UpdateToken failed: token in wrong state, ipcToken={}. Returning redirectUrl={}", auditLogPrefix, isolationToken.get(), websiteUrlWithQuery);
        }

        return new TokenUpdateResponse(websiteUrlWithQuery);
    }
}
