@file:Suppress("TestFunctionName")

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
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.analyticslogs.LogInsightsAnalyticsService.Companion.isInWindow
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TokenAgeRange
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.asString
import uk.nhs.nhsx.testhelper.assertions.S3ObjectAssertions.content
import uk.nhs.nhsx.testhelper.data.TestData.expectedCircuitBreakerLogs
import uk.nhs.nhsx.testhelper.data.TestData.expectedCtaExchangeStats
import uk.nhs.nhsx.testhelper.data.TestData.expectedCtaTokenGenStats
import uk.nhs.nhsx.testhelper.data.TestData.expectedCtaTokenStatusStats
import uk.nhs.nhsx.testhelper.data.TestData.expectedInterOpDownloadStats
import uk.nhs.nhsx.testhelper.data.TestData.expectedInterOpUploadStats
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.getBucket
import uk.nhs.nhsx.testhelper.mocks.getObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.*

class LogInsightsAnalyticsServiceTest {
    private val events = RecordingEvents()
    private val now = Instant.parse("2021-01-24T01:00:00Z")
    private val serviceWindow = ServiceWindow(now)
    private val objectKeyNameProvider = { UUID(0, 0).let(UUID::toString).let(ObjectKey::of) }
    private val awsS3 = FakeS3()

    @Test
    fun `retrieves cloudwatch logs`() {
        val client = mockk<AWSLogs> {
            every { startQuery(any()) } returns StartQueryResult().withQueryId("0")
            every { getQueryResults(any()) } returns resultWithStatus("Complete")
        }

        val results = LogInsightsAnalyticsService(client)
            .executeCloudWatchInsightQuery(serviceWindow)

        expectThat(results).isEqualTo(expectedCircuitBreakerLogs)
    }

    @Test
    fun `waits until query is in Complete state`() {
        val client = mockk<AWSLogs> {
            every { startQuery(any()) } returns StartQueryResult().withQueryId("0")
            every { getQueryResults(any()) } returns
                resultWithStatus("Scheduled") andThen resultWithStatus("Running") andThen resultWithStatus("Complete")
        }

        val results = LogInsightsAnalyticsService(client)
            .executeCloudWatchInsightQuery(serviceWindow)

        expectThat(results).isEqualTo(expectedCircuitBreakerLogs)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Cancelled", "Failed", "Timeout", "Unknown"])
    fun `fails when query status is not Complete`(status: String) {
        val client = mockk<AWSLogs> {
            every { startQuery(any()) } returns StartQueryResult().withQueryId("0")
            every { getQueryResults(any()) } returns
                resultWithStatus("Scheduled") andThen resultWithStatus("Running") andThen resultWithStatus(status)
        }

        expectCatching {
            LogInsightsAnalyticsService(client)
                .executeCloudWatchInsightQuery(serviceWindow)
        }.isFailure().isA<RuntimeException>()
    }

    @Test
    fun `convert circuit breaker logs`() {
        val expectedList = listOf(
            CircuitBreakerStats(
                startOfHour = "2021-01-20 16:00:00.000",
                exposureNotificationCBCount = 324,
                iosExposureNotificationCBCount = 143,
                androidExposureNotificationCBCount = 181,
                uniqueRequestIds = 0,
                appVersion = "0.1.2"
            ),
            CircuitBreakerStats(
                startOfHour = "2021-01-20 15:00:00.000",
                exposureNotificationCBCount = 324,
                iosExposureNotificationCBCount = 143,
                androidExposureNotificationCBCount = 181,
                uniqueRequestIds = 0,
                appVersion = "3.4.5"
            ),
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
            KeyFederationDownloadStats(
                startOfHour = "2021-01-20 16:00:00.000",
                origin = "GB-SCO",
                testType = 0,
                numberOfKeysDownloaded = 100,
                numberOfKeysImported = 100
            ),
            KeyFederationDownloadStats(
                startOfHour = "2021-01-20 17:00:00.000",
                origin = "JE",
                testType = 1,
                numberOfKeysDownloaded = 101,
                numberOfKeysImported = 99
            )
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
            KeyFederationUploadStats(startOfHour = "2021-01-20 16:00:00.000", testType = 0, numberOfKeysUploaded = 100),
            KeyFederationUploadStats(startOfHour = "2021-01-20 17:00:00.000", testType = 1, numberOfKeysUploaded = 101)
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
                startDate = LocalDate.of(2021, 5, 4),
                testType = TestKit.RAPID_SELF_REPORTED,
                source = Country.England,
                resultType = TestResult.Negative,
                total = 100
            ),
            CtaTokenGenStats(
                startDate = LocalDate.of(2021, 5, 4),
                testType = TestKit.RAPID_SELF_REPORTED,
                source = Country.Wales,
                resultType = TestResult.Positive,
                total = 450
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
                startDate = LocalDate.of(2021, 5, 4),
                testType = TestKit.RAPID_SELF_REPORTED,
                platform = MobileOS.iOS,
                tokenAgeRange = TokenAgeRange.GREATER_THAN_48_HOURS,
                source = Country.England,
                total = 300,
                appVersion = "4.10"
            ),
            CtaExchangeStats(
                startDate = LocalDate.of(2021, 5, 4),
                testType = TestKit.RAPID_SELF_REPORTED,
                platform = MobileOS.Android,
                tokenAgeRange = TokenAgeRange.GREATER_THAN_48_HOURS,
                source = Country.England,
                total = 200,
                appVersion = "4.10"
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
            CtaTokenStatusStats(
                startDate = LocalDate.of(2021, 5, 4),
                testType = TestKit.RAPID_SELF_REPORTED,
                source = Country.England,
                total = 200
            ),
            CtaTokenStatusStats(
                startDate = LocalDate.of(2021, 5, 4),
                testType = TestKit.LAB_RESULT,
                source = Country.Wales,
                total = 100
            ),
        )
        val expectedFormat = """
            |{"startDate":"2021-05-04","testType":"RAPID_SELF_REPORTED","source":"England","total":200}
            |{"startDate":"2021-05-04","testType":"LAB_RESULT","source":"Wales","total":100}""".trimMargin()

        expectThat(CtaTokenStatusStatsConverter().from(expectedCtaTokenStatusStats)).isEqualTo(expectedList)
        expectThat(LogInsightsAnalyticsService.toAnalyticsJson(expectedList)).isEqualTo(expectedFormat)
    }

    @Test
    fun `should return true when time is 1am`() {
        val instantSetTo1am = Instant.from(ZonedDateTime.of(2021, 1, 1, 1, 0, 0, 0, UTC))
        expectThat(isInWindow(instantSetTo1am)).isTrue()
    }

    @Test
    fun `should return false when time is 12pm`() {
        val instantSetTo12pm = Instant.from(ZonedDateTime.of(2021, 1, 1, 12, 0, 0, 0, UTC))
        expectThat(isInWindow(instantSetTo12pm)).isFalse()
    }

    @Test
    fun `uploads to S3`() {
        val client = mockk<AWSLogs> {
            every { startQuery(any()) } returns StartQueryResult().withQueryId("0")
            every { getQueryResults(any()) } returns resultWithStatus("Complete")
        }

        LogInsightsAnalyticsService(client).generateStatisticsAndUploadToS3(now)

        expectThat(awsS3)
            .getBucket(BucketName.of("anything"))
            .hasSize(1)
            .getObject("2021/01/23/00000000-0000-0000-0000-000000000000.json")
            .content
            .asString()
            .isEqualTo("""
                {"startOfHour":"2021-01-20 16:00:00.000","exposureNotificationCBCount":324,"iosExposureNotificationCBCount":143,"androidExposureNotificationCBCount":181,"uniqueRequestIds":0,"appVersion":"0.1.2"}
                {"startOfHour":"2021-01-20 15:00:00.000","exposureNotificationCBCount":324,"iosExposureNotificationCBCount":143,"androidExposureNotificationCBCount":181,"uniqueRequestIds":0,"appVersion":"3.4.5"}
            """.trimIndent())
    }

    private fun LogInsightsAnalyticsService(client: AWSLogs) = LogInsightsAnalyticsService(
        client = client,
        events = events,
        awsS3 = awsS3,
        logGroup = "/aws/lambda/mock-exposure-notification-circuit-breaker",
        bucketName = "anything",
        logInsightsQuery = "query",
        bucketPrefix = "",
        objectKeyNameProvider = objectKeyNameProvider,
        shouldAbortIfOutsideWindow = false,
        converter = CircuitBreakerStatsConverter(),
        queryPollingDuration = Duration.ofMillis(5)
    )

    private fun resultWithStatus(status: String) = GetQueryResultsResult()
        .withResults(expectedCircuitBreakerLogs)
        .withStatus(status)
}
