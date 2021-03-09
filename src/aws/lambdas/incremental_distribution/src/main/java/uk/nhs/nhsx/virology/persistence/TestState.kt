package uk.nhs.nhsx.virology.persistence

import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.TestResultPollingToken
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult
import uk.nhs.nhsx.virology.result.TestResult.Positive

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
