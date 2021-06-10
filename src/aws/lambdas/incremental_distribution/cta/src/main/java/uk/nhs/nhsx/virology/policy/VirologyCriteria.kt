package uk.nhs.nhsx.virology.policy

import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestJourney
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult

data class VirologyCriteria(
    private val testJourney: TestJourney,
    private val country: Country,
    private val testKit: TestKit,
    private val testResult: TestResult
)
