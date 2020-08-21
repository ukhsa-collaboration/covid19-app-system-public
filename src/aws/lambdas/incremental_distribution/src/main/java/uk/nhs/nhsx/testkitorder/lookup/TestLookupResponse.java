package uk.nhs.nhsx.testkitorder.lookup;

import uk.nhs.nhsx.core.DateFormatValidator;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.util.Arrays;
import java.util.List;

public class TestLookupResponse {

    private static final List<String> VALID_TEST_RESULTS = Arrays.asList("POSITIVE", "NEGATIVE", "VOID");

    public final String testEndDate;
    public final String testResult;

    public TestLookupResponse(String testEndDate, String testResult) {
        if (!DateFormatValidator.isValid(testEndDate)) {
            throw new ApiResponseException(HttpStatusCode.INTERNAL_SERVER_ERROR_500, "Unexpected date format");
        }

        if (!VALID_TEST_RESULTS.contains(testResult)) {
            throw new ApiResponseException(HttpStatusCode.INTERNAL_SERVER_ERROR_500, "Unexpected test result");
        }

        this.testEndDate = testEndDate;
        this.testResult = testResult;
    }

}
