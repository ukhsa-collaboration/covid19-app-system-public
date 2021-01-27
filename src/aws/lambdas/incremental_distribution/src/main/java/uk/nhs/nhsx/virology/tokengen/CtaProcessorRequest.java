package uk.nhs.nhsx.virology.tokengen;

import uk.nhs.nhsx.virology.result.VirologyResultValidator;

import java.util.Objects;

public class CtaProcessorRequest {

    public final String testResult;
    public final String testEndDate;
    public final String testKit;
    public final Integer numberOfTokens;

    public CtaProcessorRequest(String testResult, String testEndDate, String testKit, Integer numberOfTokens) {
        VirologyResultValidator.validateTestResult(testResult,testEndDate);
        this.testResult = testResult;
        this.testEndDate = testEndDate;
        this.testKit = testKit;
        this.numberOfTokens = numberOfTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CtaProcessorRequest that = (CtaProcessorRequest) o;
        return Objects.equals(testResult, that.testResult) &&
            Objects.equals(testEndDate, that.testEndDate) &&
            Objects.equals(testKit, that.testKit) &&
            Objects.equals(numberOfTokens, that.numberOfTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testResult, testEndDate, testKit, numberOfTokens);
    }

    @Override
    public String toString() {
        return "CtaProcessorRequest{" +
            "testResult='" + testResult + '\'' +
            ", testEndDate='" + testEndDate + '\'' +
            ", testKit='" + testKit + '\'' +
            ", numberOfTokens=" + numberOfTokens +
            '}';
    }
}
