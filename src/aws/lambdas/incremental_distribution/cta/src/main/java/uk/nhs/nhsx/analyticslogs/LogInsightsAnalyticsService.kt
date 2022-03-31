package uk.nhs.nhsx.analyticslogs

import com.amazonaws.services.logs.AWSLogs
import com.amazonaws.services.logs.model.GetQueryResultsRequest
import com.amazonaws.services.logs.model.GetQueryResultsResult
import com.amazonaws.services.logs.model.ResultField
import com.amazonaws.services.logs.model.StartQueryRequest
import uk.nhs.nhsx.analyticslogs.LogInsightsAnalyticsService.CloudWatchQueryStatus.Complete
import uk.nhs.nhsx.analyticslogs.LogInsightsAnalyticsService.CloudWatchQueryStatus.Running
import uk.nhs.nhsx.analyticslogs.LogInsightsAnalyticsService.CloudWatchQueryStatus.Scheduled
import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_JSON
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.Events
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class LogInsightsAnalyticsService(
    private val client: AWSLogs,
    private val logGroup: String,
    private val awsS3: AwsS3,
    private val bucketName: String,
    private val objectKeyNameProvider: ObjectKeyNameProvider,
    private val shouldAbortIfOutsideWindow: Boolean,
    private val events: Events,
    private val logInsightsQuery: String,
    private val converter: Converter<*>,
    private val bucketPrefix: String,
    private val queryPollingDuration: Duration = Duration.ofSeconds(1)
) {
    private enum class CloudWatchQueryStatus { Cancelled, Complete, Failed, Running, Scheduled, Timeout, Unknown }

    fun generateStatisticsAndUploadToS3(currentTime: Instant) {
        val window = ServiceWindow(currentTime)

        if (!isInWindow(currentTime)) {
            check(!shouldAbortIfOutsideWindow) { "CloudWatch Event triggered Lambda at wrong time." }
        }

        val logs = executeCloudWatchInsightQuery(window)

        when {
            logs.isEmpty() -> events(EmptyAnalyticsLogs)
            else -> {
                val stats = converter.from(logs)
                val json = toAnalyticsJson(stats)
                uploadToS3(json, currentTime)
            }
        }
    }

    fun executeCloudWatchInsightQuery(window: ServiceWindow): List<List<ResultField>> {
        events(QueryRequest(window.queryStart(), window.queryEnd(), logGroup, logInsightsQuery))
        val request = StartQueryRequest()
            .withQueryString(logInsightsQuery)
            .withLogGroupName(logGroup)
            .withStartTime(window.queryStart())
            .withEndTime(window.queryEnd())
        val result = client.startQuery(request)
        return getQueryResults(result.queryId).results
    }

    private fun getQueryResults(queryId: String): GetQueryResultsResult {
        val queryResultsRequest = GetQueryResultsRequest().withQueryId(queryId)
        var queryResult = client.getQueryResults(queryResultsRequest)

        while (queryResult.isExecutingQuery()) {
            Thread.sleep(queryPollingDuration.toMillis())
            events(AnalyticsLogsPolling)
            queryResult = client.getQueryResults(queryResultsRequest)
        }

        if (queryResult.isQueryComplete())
            return queryResult

        throw RuntimeException("Failed to execute CloudWatch logs query, status: ${queryResult.toStatus()}")
    }

    private fun GetQueryResultsResult.toStatus(): CloudWatchQueryStatus = CloudWatchQueryStatus.valueOf(status)
    private fun GetQueryResultsResult.isExecutingQuery() = toStatus() == Running || toStatus() == Scheduled
    private fun GetQueryResultsResult.isQueryComplete() = toStatus() == Complete

    private fun uploadToS3(json: String, currentTime: Instant) {
        val dateTimeFormatterSlash = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.systemDefault())
        val yesterdayDateSlash = dateTimeFormatterSlash.format(currentTime.minus(1, ChronoUnit.DAYS))

        val objectKey = when {
            bucketPrefix.isNotEmpty() -> ObjectKey.of("$bucketPrefix/$yesterdayDateSlash/")
            else -> ObjectKey.of("$yesterdayDateSlash/")
        }

        val fullObjectKey = objectKey
            .append(objectKeyNameProvider.generateObjectKeyName().toString())
            .append(".json")

        awsS3.upload(
            locator = Locator.of(BucketName.of(bucketName), fullObjectKey),
            contentType = APPLICATION_JSON,
            bytes = fromUtf8String(json)
        )
    }

    companion object {
        private val START_WINDOW = LocalTime.of(1, 0)
        private val END_WINDOW = LocalTime.of(1, 10)

        fun isInWindow(time: Instant): Boolean {
            val current = time.atZone(ZoneOffset.UTC).toLocalTime()
            return (current == START_WINDOW || current.isAfter(START_WINDOW)) && current.isBefore(END_WINDOW)
        }

        fun toAnalyticsJson(analyticsRows: List<*>): String =
            analyticsRows.joinToString("\n") { Json.toJson(it) }
    }
}
