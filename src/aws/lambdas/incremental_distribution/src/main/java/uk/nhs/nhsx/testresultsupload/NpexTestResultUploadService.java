package uk.nhs.nhsx.testresultsupload;

import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import java.time.Instant;
import java.time.Period;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;
import static uk.nhs.nhsx.testresultsupload.NpexTestResult.*;

public class NpexTestResultUploadService {

    private final NpexTestResultPersistenceService persistenceService;
    private final Supplier<Instant> systemClock;

    public NpexTestResultUploadService(NpexTestResultPersistenceService persistenceService,
                                       Supplier<Instant> systemClock) {
        this.persistenceService = persistenceService;
        this.systemClock = systemClock;
    }

    public void accept(NpexTestResult testResult) {
        NpexTestResultValidator.validateTestResult(testResult);

        switch (testResult.testResult) {
            case NPEX_POSITIVE:
                var expireAt = systemClock.get().plus(Period.ofWeeks(4)).getEpochSecond();
                var positive = NpexTestResult.Positive.from(testResult);
                persistenceService.persistPositiveTestResult(positive, expireAt);
                break;

            case NPEX_NEGATIVE:
            case NPEX_VOID:
                var nonPositive = NpexTestResult.NonPositive.from(testResult);
                persistenceService.persistNonPositiveTestResult(nonPositive);
                break;

            default:
                throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test result value");
        }
    }

}
