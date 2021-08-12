package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import java.time.Instant

object ExecutorTimedOutWaitingForShutdown : Event(EventCategory.Error)

data class DistributionBatchWindow(
    val now: Instant,
    val earliest: Instant,
    val latest: Instant
) : Event(Info)

data class EmptyZipDistributed(val name: String) : Event(Info)

data class ValidSubmission(val zipPath: String, val submissionDate: Instant, val enIntervalNumber: ENIntervalNumber) : Event(Info)

object KeysDistributed : Event(Info) {
    override fun toString(): String = this::class.qualifiedName ?: "KeysDistributed"
}
