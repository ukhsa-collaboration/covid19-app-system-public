package uk.nhs.nhsx.analyticslogs

import com.amazonaws.services.logs.AWSLogs
import com.amazonaws.services.logs.model.GetQueryResultsResult
import com.amazonaws.services.logs.model.ResultField
import com.amazonaws.services.logs.model.StartQueryResult
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.analyticslogs.LogInsightsAnalyticsService.Companion.isInWindow
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.mocks.FakeS3Storage
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class LogInsightsAnalyticsServiceTest {
    private val instantSetTo1am = Instant.ofEpochSecond(1611450000)
    private val client = mockk<AWSLogs>()
    private val s3Storage = FakeS3Storage()
    private val objectKeyNameProvider = mockk<ObjectKeyNameProvider>()
    private val events = RecordingEvents()

    private val circuitBreakerService = LogInsightsAnalyticsService(
        client,
        "/aws/lambda/mock-exposure-notification-circuit-breaker",
        s3Storage,
        "anything",
        objectKeyNameProvider,
        false,
        events,
        "query",
        CircuitBreakerStatsConverter()
    )

    private val expectedCircuitBreakerLogs = listOf(
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

    private val expectedInterOpDownloadStats = listOf(
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 16:00:00.000"),
            ResultField()
                .withField("origin")
                .withValue("GB-SCO"),
            ResultField()
                .withField("test_type")
                .withValue("0"),
            ResultField()
                .withField("number_of_keys")
                .withValue("100")
        ),
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 17:00:00.000"),
            ResultField()
                .withField("origin")
                .withValue("JE"),
            ResultField()
                .withField("test_type")
                .withValue("1"),
            ResultField()
                .withField("number_of_keys")
                .withValue("101")
        )
    )

    private val expectedInterOpUploadStats = listOf(
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 16:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("0"),
            ResultField()
                .withField("number_of_keys")
                .withValue("100")
        ),
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 17:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("1"),
            ResultField()
                .withField("number_of_keys")
                .withValue("101")
        )
    )

    @Test
    fun `retrieves cloudwatch logs`() {
        every { client.startQuery(any()) } returns StartQueryResult().withQueryId("0")
        every { client.getQueryResults(any()) } returns GetQueryResultsResult().withResults(expectedCircuitBreakerLogs).withStatus("Completed")
        assertThat(circuitBreakerService.executeCloudWatchInsightQuery(ServiceWindow(instantSetTo1am)), equalTo(expectedCircuitBreakerLogs))
    }

    @Test
    fun `convert circuit breaker logs`() {
        val expectedList = listOf(
            CircuitBreakerStats("2021-01-20 16:00:00.000", 324, 143, 181, 0),
            CircuitBreakerStats("2021-01-20 15:00:00.000", 324, 143, 181, 0),
        )
        val expectedFormat = """{"startOfHour":"2021-01-20 16:00:00.000","exposureNotificationCBCount":324,"iosExposureNotificationCBCount":143,"androidExposureNotificationCBCount":181,"uniqueRequestIds":0}
                                |{"startOfHour":"2021-01-20 15:00:00.000","exposureNotificationCBCount":324,"iosExposureNotificationCBCount":143,"androidExposureNotificationCBCount":181,"uniqueRequestIds":0}""".trimMargin()
        assertThat(CircuitBreakerStatsConverter().from(expectedCircuitBreakerLogs), equalTo(expectedList))
        assertThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList), equalTo(expectedFormat))

    }


    @Test
    fun `convert interop download logs`() {
        val expectedList = listOf(
            KeyFederationDownloadStats("2021-01-20 16:00:00.000", "GB-SCO", 0, 100),
            KeyFederationDownloadStats("2021-01-20 17:00:00.000", "JE", 1, 101)
        )
        val expectedFormat = """{"startOfHour":"2021-01-20 16:00:00.000","origin":"GB-SCO","testType":0,"numberOfKeys":100}
                                |{"startOfHour":"2021-01-20 17:00:00.000","origin":"JE","testType":1,"numberOfKeys":101}""".trimMargin()
        assertThat(KeyFederationDownloadStatsConverter().from(expectedInterOpDownloadStats), equalTo(expectedList))
        assertThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList), equalTo(expectedFormat))

    }

    @Test
    fun `convert interop upload logs`() {
        val expectedList = listOf(
            KeyFederationUploadStats("2021-01-20 16:00:00.000",  0, 100),
            KeyFederationUploadStats("2021-01-20 17:00:00.000",  1, 101)
        )
        val expectedFormat = """{"startOfHour":"2021-01-20 16:00:00.000","testType":0,"numberOfKeys":100}
                                |{"startOfHour":"2021-01-20 17:00:00.000","testType":1,"numberOfKeys":101}""".trimMargin()
        assertThat(KeyFederationUploadStatsConverter().from(expectedInterOpUploadStats), equalTo(expectedList))
        assertThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList), equalTo(expectedFormat))

    }

    @Test
    fun `should return true when time is 1am`() {
        val instantSetTo1am = Instant.from(ZonedDateTime.of(2021, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC))
        assertThat(isInWindow(instantSetTo1am), equalTo(true))
    }

    @Test
    fun `should return false when time is 12pm`() {
        val instantSetTo12pm = Instant.from(ZonedDateTime.of(2021, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC))
        assertThat(isInWindow(instantSetTo12pm), equalTo(false))
    }
}
