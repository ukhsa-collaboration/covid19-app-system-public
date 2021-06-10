package uk.nhs.nhsx.virology.persistence

import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Positive

sealed class TestState(val testResultPollingToken: TestResultPollingToken, val testKit: TestKit) {

    class PendingTestResult(testResultPollingToken: TestResultPollingToken, testKit: TestKit) :
        TestState(testResultPollingToken, testKit)

    class AvailableTestResult(
        testResultPollingToken: TestResultPollingToken,
        val testEndDate: TestEndDate,
        val testResult: TestResult,
        testKit: TestKit,
    ) : TestState(testResultPollingToken, testKit) {
        fun isPositive(): Boolean = testResult == Positive
    }
}
