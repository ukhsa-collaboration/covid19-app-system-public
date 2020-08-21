package uk.nhs.nhsx.testkitorder.order;

import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator;

import java.util.UUID;

public class TokensGenerator {

    private final CrockfordDammRandomStringGenerator linkingIdGenerator = new CrockfordDammRandomStringGenerator();

    public TestOrderTokens generateTokens() {
        return TestOrderTokens.builder()
                .withCtaToken(linkingIdGenerator.generate())
                .withTestResultPollingToken(UUID.randomUUID().toString())
                .withDiagnosisKeySubmissionToken(UUID.randomUUID().toString())
                .build();
    }

    public static class TestOrderTokens {
        public final String ctaToken;
        public final String testResultPollingToken;
        public final String diagnosisKeySubmissionToken;

        public TestOrderTokens(String ctaToken, String testResultPollingToken, String diagnosisKeySubmissionToken) {
            this.ctaToken = ctaToken;
            this.testResultPollingToken = testResultPollingToken;
            this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
        }

        static TestOrderTokensBuilder builder() {
            return new TestOrderTokensBuilder();
        }

    }

    static class TestOrderTokensBuilder {
        private String ctaToken;
        private String testResultPollingToken;
        private String diagnosisKeySubmissionToken;

        public TestOrderTokensBuilder withCtaToken(String ctaToken) {
            this.ctaToken = ctaToken;
            return this;
        }

        public TestOrderTokensBuilder withTestResultPollingToken(String testResultPollingToken) {
            this.testResultPollingToken = testResultPollingToken;
            return this;
        }

        public TestOrderTokensBuilder withDiagnosisKeySubmissionToken(String diagnosisKeySubmissionToken) {
            this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
            return this;
        }

        public TestOrderTokens build() {
            return new TestOrderTokens(ctaToken, testResultPollingToken, diagnosisKeySubmissionToken);
        }
    }

}
