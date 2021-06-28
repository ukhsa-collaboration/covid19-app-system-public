package uk.nhs.nhsx.analyticsedge.export

import com.amazonaws.services.athena.AmazonAthenaClient
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import uk.nhs.nhsx.analyticsedge.persistence.AnalyticsDao
import uk.nhs.nhsx.analyticsedge.persistence.AthenaAsyncDbClient
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.SchedulingHandler

class TriggerExportHandler(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    private val dao: AnalyticsDao = AnalyticsDao(
        workspace = environment.access.required(Environment.WORKSPACE),
        athenaOutputBucket = environment.access.required(
            Environment.EnvironmentKey.value("export_bucket_name", BucketName)
        ),
        asyncDbClient = AthenaAsyncDbClient(
            athena = AmazonAthenaClient.builder().build()
        ),
        mobileAnalyticsTable = environment.access.required(Environment.EnvironmentKey.string("mobile_analytics_table"))
    )
) : SchedulingHandler(events) {

    override fun handler() = Handler<ScheduledEvent, Event> { _, _ ->
        dao.startAdoptionDatasetQueryAsync()
        dao.startAggregateDatasetQueryAsync()
        dao.startEnpicDatasetQueryAsync()
        dao.startIsolationDatasetQueryAsync()
        dao.startPosterDatasetQueryAsync()
        ExportTriggered
    }
}

object ExportTriggered : Event(EventCategory.Info)
