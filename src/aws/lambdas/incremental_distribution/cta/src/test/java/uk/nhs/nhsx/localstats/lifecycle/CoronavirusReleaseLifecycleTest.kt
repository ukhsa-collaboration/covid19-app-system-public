package uk.nhs.nhsx.localstats.lifecycle

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.localstats.LatestAvailableRelease
import uk.nhs.nhsx.localstats.ReleaseAlreadyProcessed
import uk.nhs.nhsx.localstats.ReleaseNotSafeToDownload
import uk.nhs.nhsx.localstats.data.CoronavirusWebsite
import uk.nhs.nhsx.localstats.data.FakeCoronavirusWebsiteBackend
import uk.nhs.nhsx.localstats.data.Http
import uk.nhs.nhsx.localstats.storage.DailyLocalStatsDocumentStorage
import uk.nhs.nhsx.testhelper.assertions.contains
import java.time.Duration
import java.time.Instant

class CoronavirusReleaseLifecycleTest {

    private val events = RecordingEvents()

    @Test
    fun `trigger if daily stats does not exist`() {
        val storage = mockk<DailyLocalStatsDocumentStorage> {
            every { exists() } returns false
        }

        val lifecycle = CoronavirusReleaseLifecycle(
            api = websiteWithLatestRelease(Instant.EPOCH),
            storage = storage,
            clock = { Instant.EPOCH },
            events = events
        )

        expectThat(lifecycle)
            .get(ReleaseLifecycle::isNewReleaseAvailable)
            .isNotNull()

        expectThat(events).contains(LatestAvailableRelease::class)
    }

    @Test
    fun `trigger if new release is available (not yet processed)`() {
        val storage = mockk<DailyLocalStatsDocumentStorage> {
            every { exists() } returns true
            every { lastModified() } returns Instant.parse("2021-11-18T16:00:00Z")
        }

        val latestRelease = Instant.parse("2021-11-19T16:00:00Z")

        val lifecycle = CoronavirusReleaseLifecycle(
            api = websiteWithLatestRelease(latestRelease),
            storage = storage,
            clock = { latestRelease.plus(Duration.ofMinutes(40)) },
            events = events
        )

        expectThat(lifecycle)
            .get(ReleaseLifecycle::isNewReleaseAvailable)
            .isNotNull()

        expectThat(events).contains(LatestAvailableRelease::class)
    }

    @Test
    fun `do not trigger if new release is available (but less than 10 minutes old)`() {
        val storage = mockk<DailyLocalStatsDocumentStorage> {
            every { exists() } returns true
            every { lastModified() } returns Instant.parse("2021-11-18T16:00:00Z")
        }

        val latestRelease = Instant.parse("2021-11-19T16:35:00Z")

        val lifecycle = CoronavirusReleaseLifecycle(
            api = websiteWithLatestRelease(latestRelease),
            storage = storage,
            clock = { Instant.parse("2021-11-19T16:40:00Z") },
            events = events
        )

        expectThat(lifecycle)
            .get(ReleaseLifecycle::isNewReleaseAvailable)
            .isNull()

        expectThat(events).contains(ReleaseNotSafeToDownload::class)
    }

    @Test
    fun `do not trigger if release already processed`() {
        val storage = mockk<DailyLocalStatsDocumentStorage> {
            every { exists() } returns true
            every { lastModified() } returns Instant.parse("2021-11-19T16:01:00Z")
        }

        val latestRelease = Instant.parse("2021-11-19T16:00:00Z")

        val lifecycle = CoronavirusReleaseLifecycle(
            api = websiteWithLatestRelease(latestRelease),
            storage = storage,
            clock = { latestRelease.plus(Duration.ofMinutes(40)) },
            events = events
        )

        expectThat(lifecycle)
            .get(ReleaseLifecycle::isNewReleaseAvailable)
            .isNull()

        expectThat(events).contains(ReleaseAlreadyProcessed::class)
    }

    private fun websiteWithLatestRelease(latestRelease: Instant) =
        CoronavirusWebsite.Http(FakeCoronavirusWebsiteBackend(latestRelease))
}
