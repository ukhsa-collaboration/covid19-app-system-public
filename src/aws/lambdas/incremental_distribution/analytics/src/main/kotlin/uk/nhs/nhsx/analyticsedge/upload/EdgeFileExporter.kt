package uk.nhs.nhsx.analyticsedge.upload

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.s3.model.S3Object
import uk.nhs.nhsx.aae.S3ObjectNotFound
import uk.nhs.nhsx.analyticsedge.DataUploadSkipped
import uk.nhs.nhsx.analyticsedge.DataUploadedToEdge
import uk.nhs.nhsx.analyticsedge.Dataset
import uk.nhs.nhsx.analyticsedge.S3PutObjectEvent
import uk.nhs.nhsx.core.Json.readJsonOrNull
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.events.Event

class EdgeFileExporter(
    private val workspace: String,
    private val s3Client: AwsS3,
    private val uploader: EdgeUploader
) {

    fun export(input: SQSEvent): Event {
        if (input.records.size != 1)
            throw RuntimeException("Event must have batch size of 1")

        val sqsMessage = input.records[0]
        val message = deserializeOrThrow(sqsMessage)
        return uploadToEdge(message, sqsMessage)
    }

    private fun deserializeOrThrow(sqsMessage: SQSEvent.SQSMessage) =
        readJsonOrNull<S3PutObjectEvent>(sqsMessage.body)
            ?: throw RuntimeException(
                """SQS message parsing failed (no retry): sqsMessage.id=${sqsMessage.messageId}, body=${sqsMessage.body}"""
            )

    private fun uploadToEdge(
        event: S3PutObjectEvent,
        sqsMessage: SQSEvent.SQSMessage
    ) = when {
        event.key.value.endsWith(".metadata") -> DataUploadSkipped(sqsMessage.messageId, event.bucketName, event.key)
        else -> s3Client
            .getObject(Locator.of(event.bucketName, event.key))
            ?.let {
                val prefix = it.key.split("/").first()
                val filename = Dataset.valueOf(prefix).filename(workspace)
                uploader.uploadFile(filename, getContent(it), it.objectMetadata.contentType)
                DataUploadedToEdge(sqsMessage.messageId, event.bucketName, event.key)
            }
            ?: S3ObjectNotFound(sqsMessage.messageId, event.bucketName, event.key)
    }

    private fun getContent(obj: S3Object): ByteArray {
        obj.objectContent.use { return it.readAllBytes() }
    }

}
