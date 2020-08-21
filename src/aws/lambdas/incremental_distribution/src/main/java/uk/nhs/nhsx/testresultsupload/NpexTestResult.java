package uk.nhs.nhsx.testresultsupload;

import java.util.Objects;

public class NpexTestResult {
    
    public static final String NPEX_POSITIVE = "POSITIVE";
    public static final String NPEX_NEGATIVE = "NEGATIVE";
    public static final String NPEX_VOID = "VOID";
    
    public final String ctaToken;
    public final String testEndDate;
    public final String testResult;

    public NpexTestResult(String ctaToken, String testEndDate, String testResult) {
        this.ctaToken = ctaToken;
        this.testEndDate = testEndDate;
        this.testResult = testResult;
    }

    static class Positive extends NpexTestResult {

        private Positive(NpexTestResult testResult) {
            super(testResult.ctaToken, testResult.testEndDate, NPEX_POSITIVE);
        }

        public static Positive from(NpexTestResult testResult) {
            return new Positive(testResult);
        }
    }
    
    static class NonPositive extends NpexTestResult {

        private NonPositive(NpexTestResult testResult) {
            super(testResult.ctaToken, testResult.testEndDate, testResult.testResult);
        }

        public static NonPositive from(NpexTestResult testResult) {
            return new NonPositive(testResult);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NpexTestResult that = (NpexTestResult) o;
        return ctaToken.equals(that.ctaToken) &&
            testEndDate.equals(that.testEndDate) &&
            testResult.equals(that.testResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctaToken, testEndDate, testResult);
    }
}
