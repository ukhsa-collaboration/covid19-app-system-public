package uk.nhs.nhsx.testkitorder;

import uk.nhs.nhsx.testkitorder.lookup.TestResult;
import uk.nhs.nhsx.testkitorder.order.TokensGenerator;

import java.util.Optional;
import java.util.function.Supplier;

interface TestKitOrderPersistenceService {
    Optional<TestResult> getTestResult(TestResultPollingToken testResultPollingToken);
    TokensGenerator.TestOrderTokens persistTestOrder(Supplier<TokensGenerator.TestOrderTokens> tokens, long expireAt);
    void markForDeletion(VirologyDataTimeToLive virologyDataTimeToLive);
}
