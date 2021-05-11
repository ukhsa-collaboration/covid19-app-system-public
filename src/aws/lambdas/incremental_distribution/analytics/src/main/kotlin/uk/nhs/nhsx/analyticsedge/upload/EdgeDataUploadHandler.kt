package uk.nhs.nhsx.analyticsedge.upload

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.QueuedHandler

class EdgeDataUploadHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = SystemClock.CLOCK,
    events: Events = PrintingJsonEvents(clock),
    s3Client: AwsS3 = AwsS3Client(events),
    config: EdgeUploaderConfig = EdgeUploaderConfig.fromEnvironment(environment),
    edgeUploader: EdgeUploader = EdgeUploader(
        config,
        AwsSecretManager(AWSSecretsManagerClientBuilder.defaultClient()),
        events
    ),
    private val edgeFileExporter: EdgeFileExporter = EdgeFileExporter(s3Client, edgeUploader)
) : QueuedHandler(events) {

    override fun handler() = Handler<SQSEvent, Event> { input, _ ->
        edgeFileExporter.export(input)
    }

}
