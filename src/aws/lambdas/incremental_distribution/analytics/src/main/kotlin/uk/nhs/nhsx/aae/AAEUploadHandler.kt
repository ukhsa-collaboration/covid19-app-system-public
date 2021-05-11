package uk.nhs.nhsx.aae

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import uk.nhs.nhsx.analyticsexporter.AnalyticsFileExporter
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.QueuedHandler

/**
 * S3 PutObject -> CloudTrail -> EventBridge rule & transformation -> SQS -> Lambda: Upload of S3 object (e.g. JSON, Parquet) to AAE via HTTPS PUT
 */
class AAEUploadHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    s3Client: AwsS3 = AwsS3Client(events),
    config: AAEUploadConfig = AAEUploadConfig.fromEnvironment(environment),
    aaeUploader: AAEUploader = AAEUploader(
        config,
        AwsSecretManager(AWSSecretsManagerClientBuilder.defaultClient()),
        events
    ),
    analyticsFileExporter: AnalyticsFileExporter = AnalyticsFileExporter(
        events,
        s3Client,
        aaeUploader,
        config
    )
) : QueuedHandler(events) {
    private val handler =
        Handler<SQSEvent, Event> { input, _ -> analyticsFileExporter.export(input) }

    override fun handler() = handler


}
