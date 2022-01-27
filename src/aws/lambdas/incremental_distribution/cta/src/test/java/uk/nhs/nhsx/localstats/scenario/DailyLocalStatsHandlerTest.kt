@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.localstats.scenario

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.events.RequestRejected
import uk.nhs.nhsx.localstats.DailyLocalStats
import uk.nhs.nhsx.localstats.DailyLocalStatsDistributed
import uk.nhs.nhsx.localstats.data.CoronavirusApi
import uk.nhs.nhsx.localstats.data.CoronavirusWebsite
import uk.nhs.nhsx.localstats.data.FakeCoronavirusApiBackend
import uk.nhs.nhsx.localstats.data.FakeCoronavirusWebsiteBackend
import uk.nhs.nhsx.localstats.data.Http
import uk.nhs.nhsx.localstats.domain.DailyLocalStatsDocument
import uk.nhs.nhsx.localstats.handler.DailyLocalStatsHandler
import uk.nhs.nhsx.localstats.lifecycle.CoronavirusReleaseLifecycle
import uk.nhs.nhsx.localstats.storage.DailyLocalStatsDocumentStorage
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.assertions.contains
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS

class DailyLocalStatsHandlerTest {

    private val now = Instant.parse("2021-11-18T16:40:00Z")
    private val storage = StubDailyLocalStatsDocumentStorage(now.minus(Duration.ofDays(1)))
    private val events = RecordingEvents()

    @Test
    fun `creates local stats document`() {
        handler(storage, events)
            .handleRequest(ScheduledEvent(), TestContext())

        expectThat(storage)
            .get(StubDailyLocalStatsDocumentStorage::capturedDocument)
            .isNotNull()

        expectThat(events).contains(DailyLocalStatsDistributed::class)
    }

    @Test
    fun `maintenance mode skips document storage`() {
        handler(
            storage = storage,
            events = events,
            environment = TEST.apply(mapOf("MAINTENANCE_MODE" to "true"))
        ).handleRequest(ScheduledEvent(), TestContext())

        expectThat(storage)
            .get(StubDailyLocalStatsDocumentStorage::capturedDocument)
            .isNull()

        expectThat(events).contains(RequestRejected::class)
    }

    private fun handler(
        storage: StubDailyLocalStatsDocumentStorage,
        events: RecordingEvents,
        environment: Environment = TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "WORKSPACE" to "workspace",
                "LOCAL_STATS_BUCKET_NAME" to "bucket_name",
                "SSM_KEY_ID_PARAMETER_NAME" to "ssm_key"
            )
        )
    ) = DailyLocalStatsHandler(
        events = events,
        environment = environment,
        storage = storage,
        lifecycle = CoronavirusReleaseLifecycle(
            api = CoronavirusWebsite.Http(FakeCoronavirusWebsiteBackend(now.truncatedTo(HOURS))),
            storage = storage,
            clock = { now },
            events = events
        ),
        dailyLocalStats = DailyLocalStats(
            coronavirusApi = CoronavirusApi.Http(FakeCoronavirusApiBackend()),
            clock = { now }
        )
    )

    private class StubDailyLocalStatsDocumentStorage(private val lastModified: Instant) : DailyLocalStatsDocumentStorage {
        var capturedDocument: DailyLocalStatsDocument? = null
        override fun exists() = true
        override fun lastModified(): Instant = lastModified
        override fun put(document: DailyLocalStatsDocument) {
            capturedDocument = document
        }
    }
}
