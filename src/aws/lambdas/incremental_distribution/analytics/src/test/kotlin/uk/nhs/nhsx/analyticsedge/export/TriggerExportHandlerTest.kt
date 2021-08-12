package uk.nhs.nhsx.analyticsedge.export

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.analyticsedge.persistence.AnalyticsDao
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.handler.ScheduledEventCompleted
import uk.nhs.nhsx.core.handler.ScheduledEventStarted
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.assertions.containsExactly

class TriggerExportHandlerTest {
    private val events = RecordingEvents()

    private val dao = mockk<AnalyticsDao> {
        every { startAdoptionDatasetQueryAsync() } just io.mockk.Runs
        every { startAggregateDatasetQueryAsync() } just io.mockk.Runs
        every { startEnpicDatasetQueryAsync() } just io.mockk.Runs
        every { startIsolationDatasetQueryAsync() } just io.mockk.Runs
        every { startPosterDatasetQueryAsync() } just io.mockk.Runs
    }

    private val handler = TriggerExportHandler(
        environment = TestEnvironments.TEST.apply(
            mapOf(
                "WORKSPACE" to "workspace",
                "export_bucket_name" to "export_bucket_name"
            )
        ),
        events = events,
        dao = dao
    )

    @Test
    fun `runs export and succeeds`() {
        val response = handler.handleRequest(mockk(), TestContext())

        expectThat(response).isEqualTo(ExportTriggered.toString())
        verify(exactly = 1) { dao.startAdoptionDatasetQueryAsync() }
        verify(exactly = 1) { dao.startAggregateDatasetQueryAsync() }
        verify(exactly = 1) { dao.startEnpicDatasetQueryAsync() }
        verify(exactly = 1) { dao.startIsolationDatasetQueryAsync() }
        verify(exactly = 1) { dao.startPosterDatasetQueryAsync() }

        expectThat(events).containsExactly(
            ScheduledEventStarted::class,
            ExportTriggered::class,
            ScheduledEventCompleted::class
        )
    }
}
