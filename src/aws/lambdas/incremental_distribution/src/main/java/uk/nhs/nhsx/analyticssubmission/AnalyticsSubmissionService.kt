package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import com.amazonaws.services.kinesisfirehose.model.Record
import org.apache.http.entity.ContentType
import uk.nhs.nhsx.analyticssubmission.UploadType.Firehose
import uk.nhs.nhsx.analyticssubmission.UploadType.S3
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.analyticssubmission.model.StoredAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Jackson.toJson
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.events.Events
import java.nio.ByteBuffer

class AnalyticsSubmissionService(
    private val config: AnalyticsConfig,
    private val s3Storage: S3Storage,
    private val objectKeyNameProvider: ObjectKeyNameProvider,
    private val kinesisFirehose: AmazonKinesisFirehose,
    private val events: Events
) {
    fun accept(payload: ClientAnalyticsSubmissionPayload?) {
        val json = toJson(StoredAnalyticsSubmissionPayload.convertFrom(payload, events))
        val objectKey = objectKeyNameProvider.generateObjectKeyName()

        if (config.s3IngestEnabled) {
            uploadToS3(json, objectKey)
        }

        if (config.firehoseIngestEnabled) {
            uploadToFirehose(json, objectKey)
        }
    }

    private fun uploadToS3(json: String, objectKey: ObjectKey) {
        s3Storage.upload(
            Locator.of(config.bucketName, objectKey.append(".json")),
            ContentType.APPLICATION_JSON,
            fromUtf8String(json)
        )

        events(AnalyticsSubmissionUploaded::class.java, AnalyticsSubmissionUploaded(S3, config.firehoseStreamName, objectKey))
    }

    private fun uploadToFirehose(json: String, objectKey: ObjectKey) {
        val record = Record().withData(ByteBuffer.wrap(json.toByteArray()))
        val putRecordRequest = PutRecordRequest()
            .withRecord(record)
            .withDeliveryStreamName(config.firehoseStreamName)
        kinesisFirehose.putRecord(putRecordRequest)

        events(AnalyticsSubmissionUploaded::class.java, AnalyticsSubmissionUploaded(Firehose, config.firehoseStreamName, objectKey))
    }
}
