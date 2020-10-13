package uk.nhs.nhsx.virology.result;

import java.util.Objects;

import static uk.nhs.nhsx.virology.result.VirologyResultRequest.FIORANO_INDETERMINATE;
import static uk.nhs.nhsx.virology.result.VirologyResultRequest.NPEX_VOID;

public class VirologyTokenGenRequest {

    public final String testResult;
    public final String testEndDate;
    
    public VirologyTokenGenRequest(String testResult, String testEndDate) {
        if (FIORANO_INDETERMINATE.equals(testResult))
            testResult = NPEX_VOID;
        this.testEndDate = testEndDate;
        this.testResult = testResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirologyTokenGenRequest that = (VirologyTokenGenRequest) o;
        return Objects.equals(testEndDate, that.testEndDate) &&
            Objects.equals(testResult, that.testResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testEndDate, testResult);
    }

    @Override
    public String toString() {
        return "VirologyTokenGenRequest{" +
            "testResult='" + testResult + '\'' +
            ", testEndDate='" + testEndDate + '\'' +
            '}';
    }
}
