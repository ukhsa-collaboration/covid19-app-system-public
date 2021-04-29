package uk.nhs.nhsx.pubdash.handlers

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.handler.ScheduledEventCompleted
import uk.nhs.nhsx.core.handler.ScheduledEventStarted
import uk.nhs.nhsx.pubdash.DataExportService
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext

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

        assertThat(response).isEqualTo(ExportTriggered.toString())
        verify(exactly = 1) { service.triggerAllQueries() }

        events.containsExactly(
            ScheduledEventStarted::class,
            ExportTriggered::class,
            ScheduledEventCompleted::class
        )
    }
}
