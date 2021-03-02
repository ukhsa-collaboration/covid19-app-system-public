package uk.nhs.nhsx.circuitbreakerstats

import com.amazonaws.services.logs.AWSLogs
import com.amazonaws.services.logs.model.GetQueryResultsResult
import com.amazonaws.services.logs.model.ResultField
import com.amazonaws.services.logs.model.StartQueryResult
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.circuitbreakerstats.CircuitBreakerAnalyticsService.isInWindow
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.mocks.FakeS3Storage
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.function.Supplier

class CircuitBreakerAnalyticsServiceTest {
    private val instantSetTo1am = Instant.ofEpochSecond(1611450000)
    private val clock = Supplier {instantSetTo1am }
    private val client = mockk<AWSLogs>()
    private val s3Storage = FakeS3Storage()
    private val objectKeyNameProvider = mockk<ObjectKeyNameProvider>()
    private val events = RecordingEvents()

    private val service = CircuitBreakerAnalyticsService(
        clock,
        client,
        "/aws/lambda/mock-exposure-notification-circuit-breaker",
        s3Storage,
        "anything",
        objectKeyNameProvider,
        false,
        events
    )

    private val expectedLogs = listOf(
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 16:00:00.000"),
            ResultField()
                .withField("exposure_notification_cb_count")
                .withValue("324"),
            ResultField()
                .withField("iOS_exposure_notification_cb_count")
                .withValue("143"),
            ResultField()
                .withField("android_exposure_notification_cb_count")
                .withValue("181"),
            ResultField()
                .withField("unique_request_ids")
                .withValue("0")
        ),
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 15:00:00.000"),
            ResultField()
                .withField("exposure_notification_cb_count")
                .withValue("324"),
            ResultField()
                .withField("iOS_exposure_notification_cb_count")
                .withValue("143"),
            ResultField()
                .withField("android_exposure_notification_cb_count")
                .withValue("181"),
            ResultField()
                .withField("unique_request_ids")
                .withValue("0")
        )
    )

    @Test
    fun `retrieves cloudwatch logs`() {
        every { client.startQuery(any()) } returns StartQueryResult().withQueryId("0")
        every { client.getQueryResults(any()) } returns GetQueryResultsResult().withResults(expectedLogs).withStatus("Completed")
        assertThat(service.executeCloudWatchInsightQuery("mockQueryString"), equalTo(expectedLogs))
    }
    @Test
    fun `convert logs`() {
        val expectedFormat = """{"startOfHour":"2021-01-20 16:00:00.000","exposureNotificationCBCount":324,"iOSExposureNotificationCBCount":143,"androidExposureNotificationCBCount":181,"uniqueRequestIds":0}
                                |{"startOfHour":"2021-01-20 15:00:00.000","exposureNotificationCBCount":324,"iOSExposureNotificationCBCount":143,"androidExposureNotificationCBCount":181,"uniqueRequestIds":0}
                                |""".trimMargin()
        assertThat(service.convertToJSON(expectedLogs), equalTo(expectedFormat))
    }

    @Test
    fun `should return true when time is 1am`() {
        val instantSetTo1am = Instant.from(ZonedDateTime.of(2021, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC))
        assertThat(isInWindow(instantSetTo1am), equalTo(true) )
    }
    @Test
    fun `should return false when time is 12pm`() {
        val instantSetTo12pm = Instant.from(ZonedDateTime.of(2021, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC))
        assertThat(isInWindow(instantSetTo12pm), equalTo(false) )
    }
}
