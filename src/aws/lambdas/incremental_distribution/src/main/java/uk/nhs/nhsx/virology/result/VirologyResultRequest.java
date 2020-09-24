package uk.nhs.nhsx.virology.result;

import uk.nhs.nhsx.virology.CtaToken;

import java.util.Objects;

public class VirologyResultRequest {

    public static final String NPEX_POSITIVE = "POSITIVE";
    public static final String NPEX_NEGATIVE = "NEGATIVE";
    public static final String NPEX_VOID = "VOID";
    public static final String FIORANO_INDETERMINATE = "INDETERMINATE";

    public final CtaToken ctaToken;
    public final String testEndDate;
    public final String testResult;

    public VirologyResultRequest(String ctaToken, String testEndDate, String testResult) {
        if (FIORANO_INDETERMINATE.equals(testResult))
            testResult = NPEX_VOID;
        this.ctaToken = CtaToken.of(ctaToken);
        this.testEndDate = testEndDate;
        this.testResult = testResult;
    }

    public static class Positive extends VirologyResultRequest {

        private Positive(VirologyResultRequest testResult) {
            super(testResult.ctaToken.value, testResult.testEndDate, NPEX_POSITIVE);
        }

        public static Positive from(VirologyResultRequest testResult) {
            return new Positive(testResult);
        }
    }

    public static class NonPositive extends VirologyResultRequest {

        private NonPositive(VirologyResultRequest testResult) {
            super(testResult.ctaToken.value, testResult.testEndDate, testResult.testResult);
        }

        public static NonPositive from(VirologyResultRequest testResult) {
            return new NonPositive(testResult);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirologyResultRequest that = (VirologyResultRequest) o;
        return ctaToken.equals(that.ctaToken) &&
            testEndDate.equals(that.testEndDate) &&
            testResult.equals(that.testResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctaToken, testEndDate, testResult);
    }
}