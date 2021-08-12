package uk.nhs.nhsx.pubdash.handlers

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.handler.ScheduledEventCompleted
import uk.nhs.nhsx.core.handler.ScheduledEventStarted
import uk.nhs.nhsx.pubdash.DataExportService
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.assertions.containsExactly

class TriggerExportHandlerTest {

    private val events = RecordingEvents()

    private val service = mockk<DataExportService> {
        every { triggerAllQueries() } just Runs
    }

    private val handler = TriggerExportHandler(
        environment = TestEnvironments.environmentWith(),
        service = service,
        events = events
    )

    @Test
    fun `runs export and succeeds`() {
        val response = handler.handleRequest(mockk(), TestContext())

        expectThat(response).isEqualTo(ExportTriggered.toString())
        verify(exactly = 1) { service.triggerAllQueries() }

        expectThat(events).containsExactly(
            ScheduledEventStarted::class,
            ExportTriggered::class,
            ScheduledEventCompleted::class
        )
    }
}
