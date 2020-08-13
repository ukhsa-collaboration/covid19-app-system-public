package uk.nhs.nhsx.testresultsupload;

import java.util.Objects;

public class NPEXTestResult {
    
    public final String ctaToken;
    public final String testEndDate;
    public final String testResult;

    public NPEXTestResult(String ctaToken, String testEndDate, String testResult) {
        this.ctaToken = ctaToken;
        this.testEndDate = testEndDate;
        this.testResult = testResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NPEXTestResult that = (NPEXTestResult) o;
        return ctaToken.equals(that.ctaToken) &&
            testEndDate.equals(that.testEndDate) &&
            testResult.equals(that.testResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctaToken, testEndDate, testResult);
    }
}
