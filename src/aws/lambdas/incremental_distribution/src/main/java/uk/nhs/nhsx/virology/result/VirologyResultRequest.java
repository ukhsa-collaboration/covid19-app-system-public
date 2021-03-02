package uk.nhs.nhsx.virology.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import uk.nhs.nhsx.virology.CtaToken;
import uk.nhs.nhsx.virology.TestKit;

import java.util.Objects;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;
import static uk.nhs.nhsx.virology.TestKit.LAB_RESULT;
import static uk.nhs.nhsx.virology.TestKit.RAPID_RESULT;
import static uk.nhs.nhsx.virology.TestKit.RAPID_SELF_REPORTED;

public class VirologyResultRequest {

    public static final String NPEX_POSITIVE = "POSITIVE";
    public static final String NPEX_NEGATIVE = "NEGATIVE";
    public static final String NPEX_VOID = "VOID";
    public static final String FIORANO_INDETERMINATE = "INDETERMINATE";

    public final CtaToken ctaToken;
    public final String testEndDate;
    public final String testResult;
    public TestKit testKit;

    @JsonCreator
    public VirologyResultRequest(String ctaToken, String testEndDate, String testResult) {
        this(ctaToken, testEndDate, testResult, null);
    }

    public VirologyResultRequest(String ctaToken, String testEndDate, String testResult, TestKit testKit) {
        if (FIORANO_INDETERMINATE.equals(testResult))
            testResult = NPEX_VOID;
        this.ctaToken = CtaToken.of(ctaToken);
        this.testEndDate = testEndDate;
        this.testResult = testResult;
        this.testKit = testKit;
    }

    public static class Positive extends VirologyResultRequest {

        private Positive(VirologyResultRequest testResult) {
            super(testResult.ctaToken.value, testResult.testEndDate, NPEX_POSITIVE, testResult.testKit);
        }

        public static Positive from(VirologyResultRequest testResult) {
            return new Positive(testResult);
        }
    }

    public static class NonPositive extends VirologyResultRequest {

        private NonPositive(VirologyResultRequest testResult) {
            super(testResult.ctaToken.value, testResult.testEndDate, testResult.testResult, testResult.testKit);
        }

        public static NonPositive from(VirologyResultRequest testResult) {
            return new NonPositive(testResult);
        }
    }

    public static VirologyResultRequest v1TestKitValidator(VirologyResultRequest request) {
        final TestKit testKit = request.testKit;
        boolean isValid = testKit == null;
        if (!isValid)
            throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test type value");
        return request;
    }

    public static VirologyResultRequest v2TestKitValidator(VirologyResultRequest request) {
        final TestKit testKit = request.testKit;
        final boolean pcr = LAB_RESULT == testKit;
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
        VirologyResultRequest that = (VirologyResultRequest) o;
        return ctaToken.equals(that.ctaToken) && testEndDate.equals(that.testEndDate) && testResult.equals(that.testResult) && testKit == that.testKit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctaToken, testEndDate, testResult, testKit);
    }

    @Override
    public String toString() {
        return "VirologyResultRequest{" +
            "ctaToken=" + ctaToken +
            ", testEndDate='" + testEndDate + '\'' +
            ", testResult='" + testResult + '\'' +
            ", testKit=" + testKit +
            '}';
    }
}
