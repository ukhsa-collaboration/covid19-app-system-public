package uk.nhs.nhsx.testkitorder;

import java.util.Optional;

interface TestKitOrderPersistenceService {
    Optional<TestResult> getTestResult(TestResultPollingToken testResultPollingToken);
    void persistTestOrder(TokensGenerator.TestOrderTokens tokens);
}
