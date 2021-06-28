package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import com.amazonaws.services.kinesisfirehose.model.Record
import uk.nhs.nhsx.analyticssubmission.UploadType.Firehose
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.analyticssubmission.model.StoredAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.Events
import java.nio.ByteBuffer
import java.time.Duration

class AnalyticsSubmissionService(
    private val config: AnalyticsConfig,
    private val objectKeyNameProvider: ObjectKeyNameProvider,
    private val kinesisFirehose: AmazonKinesisFirehose,
    private val events: Events,
    private val clock: Clock
) {
    fun accept(payload: ClientAnalyticsSubmissionPayload) {
        if (payload.analyticsWindow.startDate.isAfter(clock().plus(Duration.ofDays(365))) ||
            payload.analyticsWindow.endDate.isAfter(clock().plus(Duration.ofDays(365)))
        ) {
            events(
                AnalyticsSubmissionRejected(
                    startDate = payload.analyticsWindow.startDate.toString(),
                    endDate = payload.analyticsWindow.endDate.toString(),
                    deviceModel = payload.metadata.deviceModel,
                    appVersion = payload.metadata.latestApplicationVersion,
                    osVersion = payload.metadata.operatingSystemVersion
                )
            )
            return
        }

        val json = toJson(StoredAnalyticsSubmissionPayload.convertFrom(payload, events))
        val objectKey = objectKeyNameProvider.generateObjectKeyName()

        if (config.firehoseIngestEnabled) {
            uploadToFirehose(json, objectKey)
        }
    }


    private fun uploadToFirehose(json: String, objectKey: ObjectKey) {
        val record = Record().withData(ByteBuffer.wrap(json.toByteArray()))
        val putRecordRequest = PutRecordRequest()
            .withRecord(record)
            .withDeliveryStreamName(config.firehoseStreamName)
        kinesisFirehose.putRecord(putRecordRequest)

        events(AnalyticsSubmissionUploaded(Firehose, config.firehoseStreamName, objectKey))
    }
}
