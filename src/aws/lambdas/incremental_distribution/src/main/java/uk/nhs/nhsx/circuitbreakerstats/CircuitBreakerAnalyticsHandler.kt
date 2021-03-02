package uk.nhs.nhsx.circuitbreakerstats

import com.amazonaws.services.logs.AWSLogsClientBuilder
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.UniqueId
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.UniqueObjectKeyNameProvider
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.scheduled.Scheduling
import uk.nhs.nhsx.core.scheduled.SchedulingHandler
import java.time.Instant
import java.util.function.Supplier

@Suppress("unused")
class CircuitBreakerAnalyticsHandler(private val service: CircuitBreakerAnalyticsService, events: Events) : SchedulingHandler(events) {
    @JvmOverloads
    constructor(
        environment: Environment = Environment.fromSystem(),
        clock: Supplier<Instant> = CLOCK,
        events: Events
    ) : this(circuitBreakerAnalyticsService(environment, clock, events), events)

    override fun handler() = Scheduling.Handler { _, _ ->
        service.generateStatsAndUploadToS3(queryString)
        CircuitBreakerAnalyticsUploadedToS3
    }
}

object CircuitBreakerAnalyticsUploadedToS3 : Event(EventCategory.Info)

private const val queryString = """fields @timestamp, @message
| filter @message like /Received http request.*exposure-notification\/request,requestId=.*,apiKeyName=mobile/
| parse @message /Received http request.*exposure-notification\/request,requestId=(?<requestId>.*),apiKeyName=mobile/
| parse @message /(?<iOS>=[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12},)/
| parse @message /(?<android>=[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12},)/
| stats count(android)+count(iOS) as exposure_notification_cb_count, count(iOS) as iOS_exposure_notification_cb_count, count(android) as android_exposure_notification_cb_count, count_distinct(requestId) as unique_request_ids by bin(1h) as start_of_hour"""

private fun circuitBreakerAnalyticsService(
    environment: Environment,
    clock: Supplier<Instant>,
    events: Events
): CircuitBreakerAnalyticsService {

    val logGroupName = environment.access.required(EnvironmentKey.string("CIRCUIT_BREAKER_LOG_GROUP_NAME"))
    val bucketName = environment.access.required(EnvironmentKey.string("CIRCUIT_BREAKER_ANALYTICS_BUCKET_NAME"))
    val shouldAbortIfOutsideWindow = environment.access.required(EnvironmentKey.bool("ABORT_OUTSIDE_TIME_WINDOW"))

    return CircuitBreakerAnalyticsService(
        clock,
        AWSLogsClientBuilder.defaultClient(),
        logGroupName,
        AwsS3Client(PrintingJsonEvents(CLOCK)),
        bucketName,
        UniqueObjectKeyNameProvider(clock, UniqueId.ID),
        shouldAbortIfOutsideWindow,
        events
    )
}
