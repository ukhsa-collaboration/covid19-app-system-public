package uk.nhs.nhsx.pubdash.handlers

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.scheduled.ScheduledEventCompleted
import uk.nhs.nhsx.core.scheduled.ScheduledEventStarted
import uk.nhs.nhsx.pubdash.DataExportService
import uk.nhs.nhsx.pubdash.RecordingEvents
import uk.nhs.nhsx.pubdash.TestEnvironments

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
        val response = handler.handleRequest(mockk(), mockk())

        assertThat(response).isEqualTo(ExportTriggered.toString())
        verify(exactly = 1) { service.triggerAllQueries() }

        events.containsExactly(
            ScheduledEventStarted::class,
            ExportTriggered::class,
            ScheduledEventCompleted::class
        )
    }
}
