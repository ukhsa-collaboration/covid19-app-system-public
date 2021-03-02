package uk.nhs.nhsx.virology.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.virology.TestKit;

import java.util.Objects;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;
import static uk.nhs.nhsx.virology.TestKit.LAB_RESULT;
import static uk.nhs.nhsx.virology.TestKit.RAPID_RESULT;
import static uk.nhs.nhsx.virology.TestKit.RAPID_SELF_REPORTED;
import static uk.nhs.nhsx.virology.result.VirologyResultRequest.FIORANO_INDETERMINATE;
import static uk.nhs.nhsx.virology.result.VirologyResultRequest.NPEX_POSITIVE;
import static uk.nhs.nhsx.virology.result.VirologyResultRequest.NPEX_VOID;

public class VirologyTokenGenRequest {

    public final String testResult;
    public final String testEndDate;
    public TestKit testKit;

    @JsonCreator
    public VirologyTokenGenRequest(String testResult, String testEndDate) {
        this(testResult, testEndDate, null);
    }

    public VirologyTokenGenRequest(String testResult, String testEndDate, TestKit testKit) {
        this.testResult = FIORANO_INDETERMINATE.equals(testResult) ? NPEX_VOID : testResult;
        this.testEndDate = testEndDate;
        this.testKit = testKit;
    }

    public static VirologyTokenGenRequest v1TestKitValidator(VirologyTokenGenRequest request) {
        final TestKit testKit = request.testKit;
        boolean isValid = testKit == null;
        if (!isValid)
            throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test type value");
        return request;
    }

    public static VirologyTokenGenRequest v2TestKitValidator(VirologyTokenGenRequest request) {
        final TestKit testKit = request.testKit;
        final boolean pcr = LAB_RESULT.equals(testKit);
        final boolean lfd = RAPID_RESULT == testKit || RAPID_SELF_REPORTED == testKit;
        final boolean lfdPositive = lfd && NPEX_POSITIVE.contentEquals(request.testResult);
        boolean isValid = pcr || lfdPositive;
        if (!isValid)
            throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test type value");
        return request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirologyTokenGenRequest that = (VirologyTokenGenRequest) o;
        return testResult.equals(that.testResult) && testEndDate.equals(that.testEndDate) && Objects.equals(testKit, that.testKit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testResult, testEndDate, testKit);
    }

    @Override
    public String toString() {
        return "VirologyTokenGenRequest{" +
            "testResult='" + testResult + '\'' +
            ", testEndDate='" + testEndDate + '\'' +
            ", testKit=" + testKit +
            '}';
    }
}
