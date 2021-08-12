package uk.nhs.nhsx.analyticslogs

import com.amazonaws.services.logs.AWSLogs
import com.amazonaws.services.logs.model.GetQueryResultsResult
import com.amazonaws.services.logs.model.StartQueryResult
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.analyticslogs.LogInsightsAnalyticsService.Companion.isInWindow
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TokenAgeRange
import uk.nhs.nhsx.testhelper.data.TestData.expectedCircuitBreakerLogs
import uk.nhs.nhsx.testhelper.data.TestData.expectedCtaExchangeStats
import uk.nhs.nhsx.testhelper.data.TestData.expectedCtaTokenGenStats
import uk.nhs.nhsx.testhelper.data.TestData.expectedCtaTokenStatusStats
import uk.nhs.nhsx.testhelper.data.TestData.expectedInterOpDownloadStats
import uk.nhs.nhsx.testhelper.data.TestData.expectedInterOpUploadStats
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class LogInsightsAnalyticsServiceTest {
    private val instantSetTo1am = Instant.ofEpochSecond(1611450000)
    private val serviceWindow = ServiceWindow(instantSetTo1am)
    private val client = mockk<AWSLogs>()
    private val s3Storage = FakeS3()
    private val objectKeyNameProvider = mockk<ObjectKeyNameProvider>()
    private val events = RecordingEvents()

    private val service = LogInsightsAnalyticsService(
        client = client,
        logGroup = "/aws/lambda/mock-exposure-notification-circuit-breaker",
        s3Storage = s3Storage,
        bucketName = "anything",
        objectKeyNameProvider = objectKeyNameProvider,
        shouldAbortIfOutsideWindow = false,
        events = events,
        logInsightsQuery = "query",
        converter = CircuitBreakerStatsConverter(),
        bucketPrefix = "",
        queryPollingDuration = Duration.ofMillis(5)
    )

    private fun resultWithStatus(status: String): GetQueryResultsResult =
        GetQueryResultsResult().withResults(expectedCircuitBreakerLogs).withStatus(status)

    // Query status:
    // Scheduled -> Running -> [Complete || Cancelled || Failed || Timeout || Unknown]

    @Test
    fun `retrieves cloudwatch logs`() {
        every { client.startQuery(any()) } returns StartQueryResult().withQueryId("0")
        every { client.getQueryResults(any()) } returns resultWithStatus("Complete")
        expectThat(service.executeCloudWatchInsightQuery(serviceWindow)).isEqualTo(expectedCircuitBreakerLogs)
    }

    @Test
    fun `waits until query is in Complete state`() {
        every { client.startQuery(any()) } returns StartQueryResult().withQueryId("0")
        every { client.getQueryResults(any()) } returns
            resultWithStatus("Scheduled") andThen resultWithStatus("Running") andThen resultWithStatus("Complete")
        expectThat(service.executeCloudWatchInsightQuery(serviceWindow)).isEqualTo(expectedCircuitBreakerLogs)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Cancelled", "Failed", "Timeout", "Unknown"])
    fun `fails when query status is not Complete`(status: String) {
        every { client.startQuery(any()) } returns StartQueryResult().withQueryId("0")
        every { client.getQueryResults(any()) } returns
            resultWithStatus("Scheduled") andThen resultWithStatus("Running") andThen resultWithStatus(status)

        expectCatching { service.executeCloudWatchInsightQuery(serviceWindow) }
            .isFailure()
            .isA<RuntimeException>()
    }

    @Test
    fun `convert circuit breaker logs`() {
        val expectedList = listOf(
            CircuitBreakerStats("2021-01-20 16:00:00.000", 324, 143, 181, 0, "0.1.2"),
            CircuitBreakerStats("2021-01-20 15:00:00.000", 324, 143, 181, 0, "3.4.5"),
        )
        val expectedFormat = """
            |{"startOfHour":"2021-01-20 16:00:00.000","exposureNotificationCBCount":324,"iosExposureNotificationCBCount":143,"androidExposureNotificationCBCount":181,"uniqueRequestIds":0,"appVersion":"0.1.2"}
            |{"startOfHour":"2021-01-20 15:00:00.000","exposureNotificationCBCount":324,"iosExposureNotificationCBCount":143,"androidExposureNotificationCBCount":181,"uniqueRequestIds":0,"appVersion":"3.4.5"}""".trimMargin()
        expectThat(CircuitBreakerStatsConverter().from(expectedCircuitBreakerLogs)).isEqualTo(expectedList)
        expectThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList)).isEqualTo(expectedFormat)
    }

    @Test
    fun `convert interop download logs`() {
        val expectedList = listOf(
            KeyFederationDownloadStats("2021-01-20 16:00:00.000", "GB-SCO", 0, 100, 100),
            KeyFederationDownloadStats("2021-01-20 17:00:00.000", "JE", 1, 101, 99)
        )
        val expectedFormat = """
            |{"startOfHour":"2021-01-20 16:00:00.000","origin":"GB-SCO","testType":0,"numberOfKeysDownloaded":100,"numberOfKeysImported":100}
            |{"startOfHour":"2021-01-20 17:00:00.000","origin":"JE","testType":1,"numberOfKeysDownloaded":101,"numberOfKeysImported":99}""".trimMargin()
        expectThat(KeyFederationDownloadStatsConverter().from(expectedInterOpDownloadStats)).isEqualTo(expectedList)
        expectThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList)).isEqualTo(expectedFormat)
    }

    @Test
    fun `convert interop upload logs`() {
        val expectedList = listOf(
            KeyFederationUploadStats("2021-01-20 16:00:00.000", 0, 100),
            KeyFederationUploadStats("2021-01-20 17:00:00.000", 1, 101)
        )
        val expectedFormat = """
            |{"startOfHour":"2021-01-20 16:00:00.000","testType":0,"numberOfKeysUploaded":100}
            |{"startOfHour":"2021-01-20 17:00:00.000","testType":1,"numberOfKeysUploaded":101}""".trimMargin()
        expectThat(KeyFederationUploadStatsConverter().from(expectedInterOpUploadStats)).isEqualTo(expectedList)
        expectThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList)).isEqualTo(expectedFormat)
    }

    @Test
    fun `convert cta token gen stats logs`() {
        val expectedList = listOf(
            CtaTokenGenStats(
                LocalDate.of(2021, 5, 4),
                TestKit.RAPID_SELF_REPORTED,
                Country.England,
                TestResult.Negative,
                100
            ),
            CtaTokenGenStats(
                LocalDate.of(2021, 5, 4),
                TestKit.RAPID_SELF_REPORTED,
                Country.Wales,
                TestResult.Positive,
                450
            ),
        )
        val expectedFormat = """
            |{"startDate":"2021-05-04","testType":"RAPID_SELF_REPORTED","source":"England","resultType":"NEGATIVE","total":100}
            |{"startDate":"2021-05-04","testType":"RAPID_SELF_REPORTED","source":"Wales","resultType":"POSITIVE","total":450}""".trimMargin()
        expectThat(CtaTokenGenStatsConverter().from(expectedCtaTokenGenStats)).isEqualTo(expectedList)
        expectThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList)).isEqualTo(expectedFormat)
    }

    @Test
    fun `convert cta exchange stats logs`() {
        val expectedList = listOf(
            CtaExchangeStats(
                LocalDate.of(2021, 5, 4),
                TestKit.RAPID_SELF_REPORTED,
                MobileOS.iOS,
                TokenAgeRange.GREATER_THAN_48_HOURS,
                Country.England,
                300,
                "4.10"
            ),
            CtaExchangeStats(
                LocalDate.of(2021, 5, 4),
                TestKit.RAPID_SELF_REPORTED,
                MobileOS.Android,
                TokenAgeRange.GREATER_THAN_48_HOURS,
                Country.England,
                200,
                "4.10"
            ),
        )
        val expectedFormat = """
            |{"startDate":"2021-05-04","testType":"RAPID_SELF_REPORTED","platform":"iOS","tokenAgeRange":"GREATER_THAN_48_HOURS","source":"England","total":300,"appVersion":"4.10"}
            |{"startDate":"2021-05-04","testType":"RAPID_SELF_REPORTED","platform":"Android","tokenAgeRange":"GREATER_THAN_48_HOURS","source":"England","total":200,"appVersion":"4.10"}""".trimMargin()
        expectThat(CtaExchangeStatsConverter().from(expectedCtaExchangeStats)).isEqualTo(expectedList)
        expectThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList)).isEqualTo(expectedFormat)
    }

    @Test
    fun `convert cta token status stats logs`() {
        val expectedList = listOf(
            CtaTokenStatusStats(LocalDate.of(2021, 5, 4), TestKit.RAPID_SELF_REPORTED, Country.England, 200),
            CtaTokenStatusStats(LocalDate.of(2021, 5, 4), TestKit.LAB_RESULT, Country.Wales, 100),
        )
        val expectedFormat = """
            |{"startDate":"2021-05-04","testType":"RAPID_SELF_REPORTED","source":"England","total":200}
            |{"startDate":"2021-05-04","testType":"LAB_RESULT","source":"Wales","total":100}""".trimMargin()
        expectThat(CtaTokenStatusStatsConverter().from(expectedCtaTokenStatusStats)).isEqualTo(expectedList)
        expectThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList)).isEqualTo(expectedFormat)
    }

    @Test
    fun `should return true when time is 1am`() {
        val instantSetTo1am = Instant.from(ZonedDateTime.of(2021, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC))
        expectThat(isInWindow(instantSetTo1am)).isTrue()
    }

    @Test
    fun `should return false when time is 12pm`() {
        val instantSetTo12pm = Instant.from(ZonedDateTime.of(2021, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC))
        expectThat(isInWindow(instantSetTo12pm)).isFalse()
    }
}
