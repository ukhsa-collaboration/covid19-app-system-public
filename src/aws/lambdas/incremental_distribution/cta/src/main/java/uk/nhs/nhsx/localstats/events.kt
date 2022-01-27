package uk.nhs.nhsx.localstats

import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info
import java.time.Instant

object DailyLocalStatsDistributed : Event(Info)
object SkippedDailyLocalStats : Event(Info)

data class LatestAvailableRelease(
    val release: Instant
) : Event(Info)

data class ReleaseNotSafeToDownload(
    val release: Instant,
    val now: Instant
) : Event(Info)

data class ReleaseAlreadyProcessed(
    val release: Instant,
    val lastModified: Instant
) : Event(Info)
