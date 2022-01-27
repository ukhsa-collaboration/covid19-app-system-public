@file:Suppress("unused")

package uk.nhs.nhsx.localstats.handler

import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.value
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.RequestRejected
import uk.nhs.nhsx.core.handler.SchedulingHandler
import uk.nhs.nhsx.core.routing.isMaintenanceModeEnabled
import uk.nhs.nhsx.localstats.DailyLocalStats
import uk.nhs.nhsx.localstats.DailyLocalStatsDistributed
import uk.nhs.nhsx.localstats.SkippedDailyLocalStats
import uk.nhs.nhsx.localstats.data.CoronavirusApi
import uk.nhs.nhsx.localstats.data.CoronavirusWebsite
import uk.nhs.nhsx.localstats.data.Http
import uk.nhs.nhsx.localstats.lifecycle.CoronavirusReleaseLifecycle
import uk.nhs.nhsx.localstats.lifecycle.ReleaseLifecycle
import uk.nhs.nhsx.localstats.storage.DailyLocalStatsDocumentStorage
import uk.nhs.nhsx.localstats.storage.S3DailyLocalStatsDocumentStorage

class DailyLocalStatsHandler(
    clock: Clock = SystemClock.CLOCK,
    events: Events = PrintingJsonEvents(clock),
    handler: HttpHandler = JavaHttpClient(),
    private val environment: Environment = Environment.fromSystem(),
    private val storage: DailyLocalStatsDocumentStorage = documentStorageFrom(environment, clock),
    private val lifecycle: ReleaseLifecycle = CoronavirusReleaseLifecycle(
        api = CoronavirusWebsite.Http(handler),
        storage = storage,
        clock = clock,
        events = events
    ),
    private val dailyLocalStats: DailyLocalStats = DailyLocalStats(CoronavirusApi.Http(handler), clock)
) : SchedulingHandler(events) {

    override fun handler() = filteringMaintenanceMode { _, _ ->
        val releaseDate = lifecycle.isNewReleaseAvailable()
        when {
            releaseDate != null -> {
                storage.put(dailyLocalStats.generateDocument(releaseDate))
                DailyLocalStatsDistributed
            }
            else -> SkippedDailyLocalStats
        }
    }

    private fun filteringMaintenanceMode(delegate: Handler<ScheduledEvent, Event>) =
        when {
            environment.isMaintenanceModeEnabled() -> Handler { _, _ -> RequestRejected("MAINTENANCE_MODE") }
            else -> delegate
        }
}

private fun documentStorageFrom(env: Environment, clock: Clock) = S3DailyLocalStatsDocumentStorage(
    bucketName = env.access.required(value("LOCAL_STATS_BUCKET_NAME", BucketName)),
    amazonS3 = AmazonS3ClientBuilder.defaultClient(),
    signer = StandardSigningFactory(
        clock = clock,
        parameters = AwsSsmParameters(),
        client = AWSKMSClientBuilder.defaultClient()
    ).datedSigner(env)
)
