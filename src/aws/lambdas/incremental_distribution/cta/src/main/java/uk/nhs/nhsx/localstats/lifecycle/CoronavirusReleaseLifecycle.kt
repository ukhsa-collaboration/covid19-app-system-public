package uk.nhs.nhsx.localstats.lifecycle

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.localstats.LatestAvailableRelease
import uk.nhs.nhsx.localstats.ReleaseAlreadyProcessed
import uk.nhs.nhsx.localstats.ReleaseNotSafeToDownload
import uk.nhs.nhsx.localstats.data.CoronavirusWebsite
import uk.nhs.nhsx.localstats.data.actions.LatestReleaseTimestamp
import uk.nhs.nhsx.localstats.domain.ReleaseDate
import uk.nhs.nhsx.localstats.storage.DailyLocalStatsDocumentStorage

class CoronavirusReleaseLifecycle(
    private val api: CoronavirusWebsite,
    private val storage: DailyLocalStatsDocumentStorage,
    private val clock: Clock,
    private val events: Events
) : ReleaseLifecycle {
    override fun isNewReleaseAvailable(): ReleaseDate? {
        val now = clock()

        val latestRelease = api(LatestReleaseTimestamp)
        events(LatestAvailableRelease(latestRelease.value))

        val isSafeToDownload = latestRelease.isSafeToDownload(now)
        if (!isSafeToDownload) {
            events(ReleaseNotSafeToDownload(latestRelease.value, now))
            return null
        }

        if (!storage.exists()) return latestRelease

        val lastModified = storage.lastModified()
        val isNew = latestRelease.isAfter(lastModified)
        return if (isNew) latestRelease else {
            events(ReleaseAlreadyProcessed(latestRelease.value, lastModified))
            null
        }
    }
}
