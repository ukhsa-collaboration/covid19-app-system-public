package uk.nhs.nhsx.aae

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.Jackson.readJson
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.queued.QueuedHandler
import java.time.Instant
import java.util.function.Supplier

/**
 * S3 PutObject -> CloudTrail -> EventBridge rule & transformation -> SQS -> Lambda: Upload of S3 object (e.g. JSON, Parquet) to AAE via HTTPS PUT
 */
class AAEUploadHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Supplier<Instant> = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    s3Client: AwsS3 = AwsS3Client(events),
    aaeUploader: AAEUploader = AAEUploader(
        AAEUploadConfig.fromEnvironment(environment),
        AwsSecretManager(AWSSecretsManagerClientBuilder.defaultClient()),
        events
    )
) : QueuedHandler(events) {
    private val handler = object : Handler<SQSEvent, Event> {
        override operator fun invoke(input: SQSEvent, context: Context): Event {
            if (input.records.size == 1) {
                val sqsMessage = input.records[0]
                val event: TransformedS3PutObjectCloudTrailEvent
                try {
                    event = readJson(sqsMessage.body, TransformedS3PutObjectCloudTrailEvent::class.java)
                } catch (e: Exception) { // -> no retry
                    return ExceptionThrown(
                        RuntimeException(
                            "SQS message parsing failed (no retry): sqsMessage.id=" + sqsMessage.messageId + ", body=" + sqsMessage.body,
                            e
                        )
                    )
                }
                return if (event.key.value.startsWith(FORMAT_CONVERSION_FAILED_PREFIX)) {
                    S3ToParquetObjectConversionFailure(sqsMessage.messageId, event.bucketName, event.key)
                } else {
                    try {
                        val s3Object = s3Client.getObject(Locator.of(event.bucketName, event.key))
                        when {
                            !s3Object.isEmpty -> {
                                aaeUploader.uploadToAAE(s3Object.get())
                                AAEDataUploadedToS3(sqsMessage.messageId, event.bucketName, event.key)
                            }
                            else -> S3ObjectNotFound(sqsMessage.messageId, event.bucketName, event.key)
                        }
                    } catch (e: Exception) { // -> retry o
                        throw RuntimeException(
                            "S3 object NOT uploaded to AAE (retry candidate): sqsMessage.id=" +
                                sqsMessage.messageId + ", body=" + sqsMessage.body, e
                        ) // r DLQ
                    }
                }
            } else {
                val event =
                    ExceptionThrown<RuntimeException>(IllegalStateException(".tf configuration error: batch_size != 1"))
                events(AAEUploadHandler::class.java, event)
                throw event.exception
            }
        }
    }

    override fun handler() = handler

    companion object {
        private const val FORMAT_CONVERSION_FAILED_PREFIX = "format-conversion-failed/"
    }

}
