package uk.nhs.nhsx.virology.persistence

import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResultPollingToken

sealed class TestState {
    abstract val testResultPollingToken: TestResultPollingToken
    abstract val testKit: TestKit

    data class PendingTestResult(
        override val testResultPollingToken: TestResultPollingToken,
        override val testKit: TestKit
    ) : TestState()

    data class AvailableTestResult(
        override val testResultPollingToken: TestResultPollingToken,
        val testEndDate: TestEndDate,
        val testResult: TestResult,
        override val testKit: TestKit,
    ) : TestState() {
        fun isPositive() = testResult == Positive
    }
}
