package uk.nhs.nhsx.analyticslogs

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import java.time.Instant

class LogInsightsAnalyticsHandlerTest {

    private val events = RecordingEvents()
    private val analyticsService = mockk<LogInsightsAnalyticsService>()
    private val instantSetTo1am = Instant.ofEpochSecond(1611450000)

    @Test
    fun `generate stats and upload to s3`() {
        every { analyticsService.generateStatisticsAndUploadToS3(instantSetTo1am) } just runs

        val handler = object : LogInsightsAnalyticsHandler(analyticsService, events) {}
        val event = ScheduledEvent().withTime(DateTime(instantSetTo1am.toEpochMilli()))
        val response = handler.handleRequest(event, TestContext())

        expectThat(response).contains(AnalyticsLogsFinished::class.java.simpleName)
        verify(exactly = 1) {
            analyticsService.generateStatisticsAndUploadToS3(instantSetTo1am)
        }
    }
}
