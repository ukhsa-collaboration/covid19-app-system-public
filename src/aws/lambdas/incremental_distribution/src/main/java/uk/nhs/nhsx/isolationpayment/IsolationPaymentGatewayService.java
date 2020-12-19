package uk.nhs.nhsx.isolationpayment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.DateFormatValidator;
import uk.nhs.nhsx.isolationpayment.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

public class IsolationPaymentGatewayService {
    
    private static final Logger logger = LogManager.getLogger(IsolationPaymentGatewayService.class);
    
    private final Supplier<Instant> systemClock;
    private final IsolationPaymentPersistence persistence;
    private final String auditLogPrefix;

    public IsolationPaymentGatewayService(Supplier<Instant> systemClock,
                                          IsolationPaymentPersistence persistence,
                                          String auditLogPrefix) {
        this.systemClock = systemClock;
        this.persistence = persistence;
        this.auditLogPrefix = auditLogPrefix;
    }

    public IsolationResponse verifyIsolationToken(String ipcToken) {
        Optional<IsolationToken> isolationToken;
        try {
            isolationToken = persistence.getIsolationToken(ipcToken);
        }
        catch (Exception e) {
            logger.error("{} VerifyToken exception: tokenId={}", auditLogPrefix, ipcToken, e);
            throw new RuntimeException(e);
        }

        if (!isolationToken.isPresent()) {
            logger.info("{} VerifyToken failed: token not found, tokenId={}", auditLogPrefix, ipcToken);
            return new IsolationResponse(ipcToken, TokenStateExternal.EXT_INVALID.value);
        }

        if (!TokenStateInternal.INT_UPDATED.value.equals(isolationToken.get().tokenStatus)) {
            logger.info("{} VerifyToken failed: token in wrong state, ipcToken={}", auditLogPrefix, isolationToken.get());
            return new IsolationResponse(ipcToken, TokenStateExternal.EXT_INVALID.value);
        }

        var updatedToken = IsolationToken.clonedToken(isolationToken.get());
        updatedToken.validatedTimestamp = systemClock.get().getEpochSecond();

        try {
            persistence.updateIsolationToken(updatedToken, TokenStateInternal.INT_UPDATED);

            logger.info("{} VerifyToken successful: existing.ipcToken={}, updated.ipcToken={}", auditLogPrefix, isolationToken.get(), updatedToken);

            return new IsolationResponse(
                ipcToken,
                TokenStateExternal.EXT_VALID.value,
                convertToString(updatedToken.riskyEncounterDate),
                convertToString(updatedToken.isolationPeriodEndDate),
                convertToString(updatedToken.createdTimestamp),
                convertToString(updatedToken.updatedTimestamp)
            );
        }
        catch (Exception e) {
            logger.error("{} VerifyToken exception: existing.ipcToken={}, !updated.ipcToken={}", auditLogPrefix, isolationToken.get(), updatedToken, e);
            throw new RuntimeException(e);
        }
    }

    private String convertToString(Long date) {
        return DateFormatValidator.formatter
            .withZone(ZoneId.from(ZoneOffset.UTC))
            .format(Instant.ofEpochSecond(date));
    }

    public IsolationResponse consumeIsolationToken(String ipcToken) {
        Optional<IsolationToken> isolationToken;
        try {
            isolationToken = persistence.getIsolationToken(ipcToken);
        }
        catch (Exception e) {
            logger.error("{} ConsumeToken exception: tokenId={}", auditLogPrefix, ipcToken, e);
            throw new RuntimeException(e);
        }

        if (!isolationToken.isPresent()) {
            logger.info("{} ConsumeToken failed: token not found, tokenId={}", auditLogPrefix, ipcToken);
            return new IsolationResponse(ipcToken, TokenStateExternal.EXT_INVALID.value);
        }

        if (!TokenStateInternal.INT_UPDATED.value.equals(isolationToken.get().tokenStatus)) {
            logger.info("{} ConsumeToken failed: token in wrong state, ipcToken={}", auditLogPrefix, isolationToken.get());
            return new IsolationResponse(ipcToken, TokenStateExternal.EXT_INVALID.value);
        }

        try {
            persistence.deleteIsolationToken(ipcToken, TokenStateInternal.INT_UPDATED);

            logger.info("{} ConsumeToken successful: deleted.ipcToken={}", auditLogPrefix, isolationToken.get());

            return new IsolationResponse(ipcToken, TokenStateExternal.EXT_CONSUMED.value);
        }
        catch (Exception e) {
            logger.error("{} ConsumeToken exception: ipcToken={}", auditLogPrefix, isolationToken.get(), e);
            throw new RuntimeException(e);
        }
    }
}
