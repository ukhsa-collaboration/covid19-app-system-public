package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import com.amazonaws.services.kinesisfirehose.model.Record
import uk.nhs.nhsx.analyticssubmission.AnalyticsMapFlattener.flattenRecursively
import uk.nhs.nhsx.analyticssubmission.UploadType.Firehose
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.events.Events
import java.nio.ByteBuffer

class AnalyticsSubmissionService(
    private val config: AnalyticsConfig,
    private val kinesisFirehose: AmazonKinesisFirehose,
    private val events: Events,
    private val clock: Clock
) {
    fun accept(payload: ClientAnalyticsSubmissionPayload) {
        when {
            payload.analyticsWindow.isDateRangeInvalid(clock) -> {
                with(payload) {
                    events(
                        AnalyticsSubmissionRejected(
                            startDate = analyticsWindow.startDate.toString(),
                            endDate = analyticsWindow.endDate.toString(),
                            deviceModel = metadata.deviceModel,
                            appVersion = metadata.latestApplicationVersion,
                            osVersion = metadata.operatingSystemVersion
                        )
                    )
                }
            }
            config.firehoseIngestEnabled -> {
                val scrubbed = MetricsScrubber(events, clock, config.policyConfig).scrub(payload)
                val flattened = flattenRecursively(scrubbed)
                val json = toJson(flattened)
                uploadToFirehose(json)
                events(AnalyticsSubmissionUploaded(Firehose, config.firehoseStreamName))
            }
            else -> events(AnalyticsSubmissionDisabled)
        }
    }

    private fun uploadToFirehose(json: String) {
        val record = Record().withData(ByteBuffer.wrap(json.toByteArray()))
        val putRecordRequest = PutRecordRequest()
            .withRecord(record)
            .withDeliveryStreamName(config.firehoseStreamName)
        kinesisFirehose.putRecord(putRecordRequest)
    }
}
