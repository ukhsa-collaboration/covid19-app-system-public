package uk.nhs.nhsx.testresultsupload;

import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;

public class NPEXTestResultUploadService {

    private final NPEXTestResultPersistenceService persistenceService;

    public NPEXTestResultUploadService(NPEXTestResultPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public void accept(NPEXTestResult testResult) {
        NPEXTestResultValidator.validateTestResult(testResult);
        if ("POSITIVE".contentEquals(testResult.testResult)) {
            persistenceService.persistPositiveTestResult(testResult);
        } else if ("NEGATIVE".contentEquals(testResult.testResult) || "VOID".contentEquals(testResult.testResult)) {
            persistenceService.persistNegativeOrVoidTestResult(testResult);
        } else {
            throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test result value");
        }
    }

}
