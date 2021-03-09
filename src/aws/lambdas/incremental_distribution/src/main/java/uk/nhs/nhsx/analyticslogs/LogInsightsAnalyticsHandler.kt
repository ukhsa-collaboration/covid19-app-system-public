package uk.nhs.nhsx.analyticslogs

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.logs.AWSLogsClientBuilder
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.UniqueId
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.UniqueObjectKeyNameProvider
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.scheduled.SchedulingHandler
import java.time.Instant
import java.util.function.Supplier

abstract class LogInsightsAnalyticsHandler(
    private val service: LogInsightsAnalyticsService,
    events: Events
) : SchedulingHandler(events) {
    override fun handler() = Handler<ScheduledEvent, Event> { scheduledEvent, _ ->
        service.generateStatisticsAndUploadToS3(Instant.ofEpochMilli( scheduledEvent.time.millis))
        AnalyticsUploadedToS3
    }
}

object AnalyticsUploadedToS3 : Event(EventCategory.Info)

fun logAnalyticsService(
    environment: Environment,
    clock: Supplier<Instant>,
    events: Events,
    queryString: String,
    converter: Converter<*>,
): LogInsightsAnalyticsService {

    val logGroupName = environment.access.required(EnvironmentKey.string("LOG_GROUP_NAME"))
    val bucketName = environment.access.required(EnvironmentKey.string("ANALYTICS_BUCKET_NAME"))
    val shouldAbortIfOutsideWindow = environment.access.required(EnvironmentKey.bool("ABORT_OUTSIDE_TIME_WINDOW"))

    return LogInsightsAnalyticsService(
        AWSLogsClientBuilder.defaultClient(),
        logGroupName,
        AwsS3Client(PrintingJsonEvents(CLOCK)),
        bucketName,
        UniqueObjectKeyNameProvider(clock, UniqueId.ID),
        shouldAbortIfOutsideWindow,
        events,
        queryString,
        converter
    )
}
