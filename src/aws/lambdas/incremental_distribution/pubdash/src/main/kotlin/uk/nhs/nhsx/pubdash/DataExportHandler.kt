package uk.nhs.nhsx.pubdash

import com.amazonaws.services.athena.AmazonAthenaClient
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.scheduled.Scheduling
import uk.nhs.nhsx.core.scheduled.SchedulingHandler
import uk.nhs.nhsx.pubdash.datasets.FakeDataSet
import java.time.Instant
import java.util.function.Supplier

class DataExportHandler(
    environment: Environment = Environment.fromSystem(),
    clock: Supplier<Instant> = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    private val service: DataExportService = DataExportService(
        bucketName = environment.access.required(EnvironmentKey.value("export_bucket_name", BucketName::of)),
        s3Storage = AwsS3Client(events),
        analyticsDataSet = FakeDataSet(
            source = AnalyticsDao(
                workspace = environment.access.required(Environment.WORKSPACE),
                dbClient = DbClient(AmazonAthenaClient.builder().build())
            )
        )
    )
) : SchedulingHandler(events) {

    override fun handler() = Scheduling.Handler { _, _ ->
        service.export()
        DataExported
    }
}

object DataExported : Event(EventCategory.Info)
