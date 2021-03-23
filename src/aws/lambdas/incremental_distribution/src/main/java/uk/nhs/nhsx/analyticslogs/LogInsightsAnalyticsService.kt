package uk.nhs.nhsx.analyticslogs

import com.amazonaws.services.logs.AWSLogs
import com.amazonaws.services.logs.model.GetQueryResultsRequest
import com.amazonaws.services.logs.model.GetQueryResultsResult
import com.amazonaws.services.logs.model.ResultField
import com.amazonaws.services.logs.model.StartQueryRequest
import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_JSON
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.events.Events
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class LogInsightsAnalyticsService(private val client: AWSLogs,
                                  private val logGroup: String,
                                  private val s3Storage: S3Storage,
                                  private val bucketName: String,
                                  private val objectKeyNameProvider: ObjectKeyNameProvider,
                                  private val shouldAbortIfOutsideWindow: Boolean,
                                  private val events: Events,
                                  private val logInsightsQuery: String,
                                  private val converter: Converter<*>) {
    fun generateStatisticsAndUploadToS3(currentTime : Instant) {
        val window = ServiceWindow(currentTime)
        if (!isInWindow(currentTime)) {
            check(!shouldAbortIfOutsideWindow) { "CloudWatch Event triggered Lambda at wrong time." }
        }
        val logs = executeCloudWatchInsightQuery(window)
        if (logs.isEmpty()) {
            events(EmptyAnalyticsLogs())
        } else {
            val stats = converter.from(logs)
            val json = toAnalyticsJson(stats)
            uploadToS3(json,currentTime)
        }
    }

    fun executeCloudWatchInsightQuery(window: ServiceWindow): List<List<ResultField>> {
        val startQueryRequest = StartQueryRequest()
            .withQueryString(logInsightsQuery)
            .withLogGroupName(logGroup)
            .withStartTime(window.queryStart())
            .withEndTime(window.queryEnd())
        val startQueryResult = client.startQuery(startQueryRequest)
        return getQueryResults(startQueryResult.queryId).results
    }

    private fun getQueryResults(queryId: String): GetQueryResultsResult {
        val queryResultsRequest = GetQueryResultsRequest().withQueryId(queryId)
        var getQueryResult = client.getQueryResults(queryResultsRequest)
        while (getQueryResult.status == "Running") {
            try {
                Thread.sleep(1000L)
                getQueryResult = client.getQueryResults(queryResultsRequest)
            } catch (e: InterruptedException) {
                events(AnalyticsLogsPollingFailed(e))
            }
        }
        return getQueryResult
    }

    private fun uploadToS3(json: String?, currentTime: Instant) {
        val dateTimeFormatterSlash = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.systemDefault())
        val yesterdayDateSlash = dateTimeFormatterSlash.format(currentTime.minus(1, ChronoUnit.DAYS))
        val objectKey = ObjectKey.of("$yesterdayDateSlash/").append(objectKeyNameProvider.generateObjectKeyName().toString()).append(".json")
        s3Storage.upload(
            Locator.of(BucketName.of(bucketName), objectKey),
            APPLICATION_JSON,
            fromUtf8String(json!!)
        )
    }

    companion object {
        private val START_WINDOW = LocalTime.of(1, 0)
        private val END_WINDOW = LocalTime.of(1, 10)
        fun isInWindow(time: Instant): Boolean {
            val current = time.atZone(ZoneOffset.UTC).toLocalTime()
            return (current == START_WINDOW || current.isAfter(START_WINDOW)) && current.isBefore(END_WINDOW)
        }

        fun toAnalyticsJson(analyticsRows: List<*>): String {
            return analyticsRows.joinToString("\n") { Jackson.toJson(it) }
        }

    }
}
