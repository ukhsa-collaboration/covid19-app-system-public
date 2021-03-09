package uk.nhs.nhsx.diagnosiskeydist.apispec

import java.time.Duration
import java.time.Instant

interface ZIPSubmissionPeriod {
    fun zipPath(): String
    fun isCoveringSubmissionDate(diagnosisKeySubmission: Instant, periodOffset: Duration): Boolean
    fun allPeriodsToGenerate(): List<ZIPSubmissionPeriod>
    val endExclusive: Instant
    val startInclusive: Instant
}
